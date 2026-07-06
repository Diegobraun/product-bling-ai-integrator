package com.loja.productbling.catalogo.application;

import com.loja.productbling.bling.infrastructure.BlingProductClient;
import com.loja.productbling.bling.infrastructure.BlingProdutoMapper;
import com.loja.productbling.ia.domain.ConteudoGerado;
import com.loja.productbling.ia.application.ConteudoIaService;
import com.loja.productbling.ia.domain.PesquisaDeProduto;
import com.loja.productbling.ia.domain.PesquisaEntrada;
import com.loja.productbling.ia.application.PesquisaProdutoIaService;
import com.loja.productbling.ia.application.VerificacaoImagemIaService;
import com.loja.productbling.catalogo.domain.Cor;
import com.loja.productbling.catalogo.domain.EntradaDeBusca;
import com.loja.productbling.catalogo.domain.Product;
import com.loja.productbling.catalogo.domain.ProductImage;
import com.loja.productbling.catalogo.domain.ProductStatus;
import com.loja.productbling.imagem.application.ImageDownloadService;
import com.loja.productbling.imagem.application.ImageProcessingService;
import com.loja.productbling.imagem.domain.ImageStorage;
import com.loja.productbling.catalogo.domain.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Service
public class ProductPipelineService {

    private static final int MAX_IMAGENS_PESQUISA = 8;
    private static final int MAX_IMAGENS_CANDIDATAS = 16;
    private static final int MAX_IMAGENS_POR_PAGINA = 10;
    private static final int MAX_PAGINAS_MARKETPLACE = 3;

    private final ProductRepository repo;
    private final ConteudoIaService conteudo;
    private final PesquisaProdutoIaService pesquisa;
    private final ImageProcessingService processamento;
    private final ImageStorage storage;
    private final ImageDownloadService downloads;
    private final VerificacaoImagemIaService verificacao;
    private final BlingProductClient bling;
    private final BlingProdutoMapper blingMapper;

    public ProductPipelineService(ProductRepository repo,
                                  ConteudoIaService conteudo,
                                  PesquisaProdutoIaService pesquisa,
                                  ImageProcessingService processamento,
                                  ImageStorage storage,
                                  ImageDownloadService downloads,
                                  VerificacaoImagemIaService verificacao,
                                  BlingProductClient bling,
                                  BlingProdutoMapper blingMapper) {
        this.repo = repo;
        this.conteudo = conteudo;
        this.pesquisa = pesquisa;
        this.processamento = processamento;
        this.storage = storage;
        this.downloads = downloads;
        this.verificacao = verificacao;
        this.bling = bling;
        this.blingMapper = blingMapper;
    }

    public Product criarPorPesquisa(PesquisaEntrada entrada) {
        Product produto = new Product();
        produto.setDadosBrutos("Produto: " + entrada.nome());
        produto.setCategoria(entrada.tipo());
        produto.setMarca(entrada.marca());
        produto.setModelo(entrada.modelo());
        produto.setEan(entrada.ean());
        produto.setSku(entrada.sku());
        produto.setFornecedor(entrada.fornecedor());
        produto.setFabricante(entrada.fabricante());
        repo.save(produto);

        try {
            aplicarPesquisa(produto, pesquisa.pesquisar(entrada), entrada.nome());
        } catch (Exception e) {
            produto.setDadosBrutos(produto.getDadosBrutos()
                    + "\n\n[Falha na pesquisa automática: " + e.getMessage() + "]");
            repo.save(produto);
        }
        return produto;
    }

    @Transactional
    public ImageProcessingService.Resultado adicionarImagem(Product produto, byte[] arquivo) throws IOException {
        ImageProcessingService.Resultado resultado = processamento.processar(arquivo, true);
        persistirImagem(produto, resultado);
        return resultado;
    }

    @Transactional
    public boolean adicionarImagemVerificada(Product produto, byte[] arquivo, String descricao, String cor)
            throws IOException {
        ImageProcessingService.Resultado semMargem = processamento.processar(arquivo, false);
        String base64 = Base64.getEncoder().encodeToString(semMargem.jpeg());
        VerificacaoImagemIaService.Classificacao classificacao =
                verificacao.classificar(descricao, cor, base64, "image/jpeg");
        if (classificacao == VerificacaoImagemIaService.Classificacao.REJEITAR) {
            return false;
        }
        ImageProcessingService.Resultado resultado =
                classificacao == VerificacaoImagemIaService.Classificacao.LIMPA
                        ? processamento.processar(arquivo, true)
                        : semMargem;
        persistirImagem(produto, resultado);
        return true;
    }

    private void persistirImagem(Product produto, ImageProcessingService.Resultado resultado) throws IOException {
        int ordem = produto.proximaOrdem();
        String url = storage.salvar(produto.getId(), ordem, resultado.jpeg());
        boolean aprovada = resultado.nitidezOk() && !resultado.upscaleNecessario();
        produto.adicionarImagem(new ProductImage(produto, url, resultado.kb(), aprovada, ordem));
        repo.save(produto);
    }

    @Transactional
    public void removerImagem(Product produto, Long imagemId) {
        produto.getImagens().stream()
                .filter(img -> imagemId.equals(img.getId()))
                .findFirst()
                .ifPresent(img -> {
                    try {
                        storage.remover(img.nomeArquivo());
                    } catch (IOException ignored) {
                    }
                });
        produto.removerImagem(imagemId);
        repo.save(produto);
    }

    @Transactional
    public void gerarConteudo(Product produto) {
        ConteudoGerado gerado = conteudo.gerar(produto, imagemBase64(produto), "image/jpeg");
        aplicar(produto, gerado);

        produto.adicionarTurno("user", "[geração inicial dos dados do produto]");
        produto.adicionarTurno("assistant", gerado.respostaBruta());

        if (produto.getStatus() == ProductStatus.RASCUNHO) {
            produto.transicionarPara(ProductStatus.GERADO);
        }
        repo.save(produto);
    }

    @Transactional
    public void revisar(Product produto, String pedido) {
        if (produto.getStatus() == ProductStatus.GERADO) {
            produto.transicionarPara(ProductStatus.EM_REVISAO);
        }
        ConteudoGerado gerado = conteudo.revisar(produto, pedido);
        aplicar(produto, gerado);
        produto.adicionarTurno("user", pedido);
        produto.adicionarTurno("assistant", gerado.respostaBruta());
        if (produto.getStatus() != ProductStatus.EM_REVISAO) {
            produto.transicionarPara(ProductStatus.EM_REVISAO);
        }
        repo.save(produto);
    }

    @Transactional
    public void salvarCampos(Product produto, String titulo, String curta, String complementar) {
        produto.setTitulo(titulo);
        produto.setDescricaoCurta(curta);
        produto.setDescricaoComplementar(complementar);
        repo.save(produto);
    }

    @Transactional
    public void aprovar(Product produto) {
        produto.transicionarPara(ProductStatus.APROVADO);
        repo.save(produto);
    }

    @Transactional
    public void publicar(Product produto) {
        try {
            String id = produto.getBlingProductId();
            if (id == null) {
                id = bling.buscarProdutoExistente(produto);
            }
            if (id != null) {
                bling.atualizarProduto(id, produto);
            } else {
                id = bling.criarProduto(produto);
            }
            produto.setBlingProductId(id);
            produto.setErroPublicacao(null);
            produto.transicionarPara(ProductStatus.PUBLICADO);
        } catch (Exception e) {
            produto.setErroPublicacao(e.getMessage());
            produto.transicionarPara(ProductStatus.ERRO_PUBLICACAO);
        }
        repo.save(produto);
    }

    @Transactional
    public Product importarDoBling(String blingId) {
        Product existente = repo.findFirstByBlingProductId(blingId).orElse(null);
        if (existente != null) {
            return existente;
        }
        Map<String, Object> dados = bling.obterProduto(blingId);
        Product produto = blingMapper.paraProduto(blingId, dados);
        repo.save(produto);
        return produto;
    }

    private void aplicarPesquisa(Product produto, PesquisaDeProduto resultado, String nomeOuLink) {
        if (resultado.dadosBrutos() != null) {
            produto.setDadosBrutos(resultado.dadosBrutos());
        }
        if (vazio(produto.getMarca())) {
            produto.setMarca(resultado.marca());
        }
        if (vazio(produto.getModelo())) {
            produto.setModelo(resultado.modelo());
        }
        if (vazio(produto.getCategoria())) {
            produto.setCategoria(resultado.categoria());
        }
        if (vazio(produto.getEan())) {
            produto.setEan(resultado.ean());
        }
        repo.save(produto);

        EntradaDeBusca entrada = EntradaDeBusca.de(nomeOuLink);
        Cor cor = entrada.cor();
        String descricao = descricaoImagem(produto, entrada);

        List<String> paginas = new ArrayList<>();
        String url = entrada.url();
        if (url != null) {
            paginas.add(url);
        }
        paginas.addAll(downloads.buscarPaginasProduto(consultaMarketplace(produto, cor), MAX_PAGINAS_MARKETPLACE));
        if (resultado.paginas() != null) {
            paginas.addAll(resultado.paginas());
        }

        List<ImageDownloadService.ImagemBaixada> candidatas = downloads.baixarCandidatas(
                resultado.imagens(), paginas, cor.tokensParaExcluir(),
                MAX_IMAGENS_CANDIDATAS, MAX_IMAGENS_POR_PAGINA);
        aplicarImagens(produto, candidatas, descricao, cor.valor());
    }

    private String consultaMarketplace(Product produto, Cor cor) {
        String base = java.util.stream.Stream.of(produto.getMarca(), produto.getModelo())
                .filter(v -> v != null && !v.isBlank())
                .map(String::strip)
                .collect(java.util.stream.Collectors.joining(" "));
        return cor.presente() ? base + " " + cor.valor() : base;
    }

    private void aplicarImagens(Product produto, List<ImageDownloadService.ImagemBaixada> candidatas,
                                String descricao, String cor) {
        int aceitas = 0;
        for (ImageDownloadService.ImagemBaixada candidata : candidatas) {
            if (aceitas >= MAX_IMAGENS_PESQUISA) {
                break;
            }
            try {
                if (adicionarImagemVerificada(produto, candidata.bytes(), descricao, cor)) {
                    aceitas++;
                }
            } catch (IOException ignored) {
            }
        }
    }

    private String descricaoImagem(Product produto, EntradaDeBusca entrada) {
        if (!entrada.ehLink() && !entrada.textoDigitado().isBlank()) {
            return entrada.textoDigitado();
        }
        return java.util.stream.Stream.of(produto.getMarca(), produto.getModelo(), produto.getCategoria())
                .filter(v -> v != null && !v.isBlank())
                .map(String::strip)
                .collect(java.util.stream.Collectors.joining(" "));
    }

    private void aplicar(Product produto, ConteudoGerado gerado) {
        if (temTexto(gerado.titulo())) {
            produto.setTitulo(gerado.titulo());
        }
        if (temTexto(gerado.descricaoComplementar())) {
            produto.setDescricaoComplementar(gerado.descricaoComplementar());
        }
        if (temTexto(gerado.descricaoCurta())) {
            produto.setDescricaoCurta(gerado.descricaoCurta());
        }
        produto.setAvaliacaoImagem(gerado.avaliacaoImagem());
    }

    private String imagemBase64(Product produto) {
        ProductImage principal = produto.getImagemPrincipal();
        if (principal == null) {
            return null;
        }
        try {
            return Base64.getEncoder().encodeToString(storage.ler(principal.nomeArquivo()));
        } catch (IOException e) {
            return null;
        }
    }

    private boolean temTexto(String valor) {
        return valor != null && !valor.isBlank();
    }

    private boolean vazio(String valor) {
        return valor == null || valor.isBlank();
    }
}

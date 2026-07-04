package com.loja.catalogbling;

import com.loja.catalogbling.bling.BlingProductClient;
import com.loja.catalogbling.claude.ClaudeContentService;
import com.loja.catalogbling.claude.ClaudeContentService.ConteudoGerado;
import com.loja.catalogbling.claude.ProductResearchService;
import com.loja.catalogbling.claude.ProductResearchService.Pesquisa;
import com.loja.catalogbling.domain.Product;
import com.loja.catalogbling.domain.ProductStatus;
import com.loja.catalogbling.imagem.ImageDownloadService;
import com.loja.catalogbling.imagem.ImageProcessingService;
import com.loja.catalogbling.imagem.ImageStorage;
import com.loja.catalogbling.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.Base64;

@Service
public class ProductPipelineService {

    private final ProductRepository repo;
    private final ClaudeContentService conteudo;
    private final ProductResearchService pesquisa;
    private final ImageProcessingService processamento;
    private final ImageStorage storage;
    private final ImageDownloadService downloads;
    private final BlingProductClient bling;

    public ProductPipelineService(ProductRepository repo,
                                  ClaudeContentService conteudo,
                                  ProductResearchService pesquisa,
                                  ImageProcessingService processamento,
                                  ImageStorage storage,
                                  ImageDownloadService downloads,
                                  BlingProductClient bling) {
        this.repo = repo;
        this.conteudo = conteudo;
        this.pesquisa = pesquisa;
        this.processamento = processamento;
        this.storage = storage;
        this.downloads = downloads;
        this.bling = bling;
    }

    public Product criarPorPesquisa(String nome) {
        Product produto = new Product();
        produto.setDadosBrutos("Produto: " + nome);
        repo.save(produto);

        try {
            aplicarPesquisa(produto, pesquisa.pesquisar(nome));
        } catch (Exception e) {
            produto.setDadosBrutos(produto.getDadosBrutos()
                    + "\n\n[Falha na pesquisa automática: " + e.getMessage() + "]");
            repo.save(produto);
        }
        return produto;
    }

    @Transactional
    public ImageProcessingService.Resultado processarImagem(Product produto, byte[] arquivo) throws IOException {
        ImageProcessingService.Resultado resultado = processamento.processar(arquivo);
        String url = storage.salvar(produto.getId(), resultado.jpeg());
        produto.setImagemUrlPublica(url);
        produto.setImagemKb(resultado.kb());
        produto.setImagemNitidez(resultado.nitidez());
        produto.setImagemAprovada(resultado.nitidezOk() && !resultado.upscaleNecessario());
        repo.save(produto);
        return resultado;
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

    private void aplicarPesquisa(Product produto, Pesquisa resultado) {
        if (resultado.dadosBrutos() != null) {
            produto.setDadosBrutos(resultado.dadosBrutos());
        }
        produto.setMarca(resultado.marca());
        produto.setModelo(resultado.modelo());
        produto.setCategoria(resultado.categoria());
        produto.setEan(resultado.ean());
        repo.save(produto);

        byte[] imagem = downloads.baixarMelhor(resultado.imagens());
        if (imagem != null) {
            try {
                processarImagem(produto, imagem);
            } catch (IOException ignored) {
            }
        }
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
        if (produto.getImagemUrlPublica() == null) {
            return null;
        }
        try {
            return Base64.getEncoder().encodeToString(storage.ler(produto.getId() + ".jpg"));
        } catch (IOException e) {
            return null;
        }
    }

    private boolean temTexto(String valor) {
        return valor != null && !valor.isBlank();
    }
}

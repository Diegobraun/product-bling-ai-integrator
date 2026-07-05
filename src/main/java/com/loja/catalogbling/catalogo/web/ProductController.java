package com.loja.catalogbling.catalogo.web;

import com.loja.catalogbling.catalogo.application.ProductPipelineService;
import com.loja.catalogbling.bling.infrastructure.BlingAuthService;
import com.loja.catalogbling.bling.infrastructure.BlingProductClient;
import com.loja.catalogbling.catalogo.domain.Product;
import com.loja.catalogbling.ia.domain.PesquisaEntrada;
import com.loja.catalogbling.imagem.application.ImageProcessingService;
import com.loja.catalogbling.catalogo.domain.ProductRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Controller
@RequestMapping("/produtos")
public class ProductController {

    private static final String FRAGMENTO_CONTEUDO = "produtos/fragmentos/conteudo :: painel";
    private static final String FRAGMENTO_IMAGEM = "produtos/fragmentos/imagem :: bloco";

    private final ProductRepository repo;
    private final ProductPipelineService pipeline;
    private final BlingAuthService bling;
    private final BlingProductClient blingProdutos;

    public ProductController(ProductRepository repo, ProductPipelineService pipeline,
                             BlingAuthService bling, BlingProductClient blingProdutos) {
        this.repo = repo;
        this.pipeline = pipeline;
        this.bling = bling;
        this.blingProdutos = blingProdutos;
    }

    @GetMapping
    public String lista(Model model) {
        model.addAttribute("produtos", repo.findAllByOrderByCriadoEmDesc());
        model.addAttribute("blingConectado", bling.conectado());
        return "produtos/lista";
    }

    @GetMapping("/novo")
    public String novoForm() {
        return "produtos/novo";
    }

    @PostMapping("/pesquisar")
    public String pesquisar(@RequestParam String nome,
                            @RequestParam(required = false) String tipo,
                            @RequestParam(required = false) String marca,
                            @RequestParam(required = false) String modelo,
                            @RequestParam(required = false) String ean,
                            @RequestParam(required = false) String sku,
                            @RequestParam(required = false) String fornecedor,
                            @RequestParam(required = false) String fabricante) {
        PesquisaEntrada entrada = new PesquisaEntrada(nome.trim(), tipo, marca, modelo,
                ean, sku, fornecedor, fabricante);
        Product produto = pipeline.criarPorPesquisa(entrada);
        return "redirect:/produtos/" + produto.getId();
    }

    @PostMapping
    public String criar(@RequestParam String dadosBrutos,
                        @RequestParam(required = false) String marca,
                        @RequestParam(required = false) String modelo,
                        @RequestParam(required = false) String categoria,
                        @RequestParam(required = false) String ean,
                        @RequestParam(required = false) String sku,
                        @RequestParam(required = false) List<MultipartFile> imagens) throws IOException {
        Product produto = new Product();
        produto.setDadosBrutos(dadosBrutos);
        produto.setMarca(marca);
        produto.setModelo(modelo);
        produto.setCategoria(categoria);
        produto.setEan(ean);
        produto.setSku(sku);
        repo.save(produto);

        adicionarImagens(produto, imagens);
        return "redirect:/produtos/" + produto.getId();
    }

    @GetMapping("/bling")
    public String listaBling(@RequestParam(defaultValue = "1") int pagina, Model model) {
        boolean conectado = bling.conectado();
        model.addAttribute("blingConectado", conectado);
        model.addAttribute("pagina", pagina);
        model.addAttribute("itens", conectado ? blingProdutos.listarProdutos(pagina) : List.of());
        return "produtos/bling";
    }

    @PostMapping("/bling/importar")
    public String importarBling(@RequestParam String blingId) {
        Product produto = pipeline.importarDoBling(blingId.trim());
        return "redirect:/produtos/" + produto.getId();
    }

    @GetMapping("/{id}")
    public String detalhe(@PathVariable Long id, Model model) {
        preencher(model, produto(id));
        return "produtos/detalhe";
    }

    @PostMapping("/{id}/imagem")
    public String enviarImagem(@PathVariable Long id, @RequestParam List<MultipartFile> imagens, Model model) throws IOException {
        Product produto = produto(id);
        adicionarImagens(produto, imagens);
        model.addAttribute("p", produto);
        return FRAGMENTO_IMAGEM;
    }

    @PostMapping("/{id}/imagem/{imagemId}/remover")
    public String removerImagem(@PathVariable Long id, @PathVariable Long imagemId, Model model) {
        Product produto = produto(id);
        pipeline.removerImagem(produto, imagemId);
        model.addAttribute("p", produto);
        return FRAGMENTO_IMAGEM;
    }

    @PostMapping("/{id}/gerar")
    public String gerar(@PathVariable Long id, Model model) {
        Product produto = produto(id);
        pipeline.gerarConteudo(produto);
        return painel(model, produto);
    }

    @PostMapping("/{id}/revisar")
    public String revisar(@PathVariable Long id, @RequestParam String pedido, Model model) {
        Product produto = produto(id);
        pipeline.revisar(produto, pedido);
        return painel(model, produto);
    }

    @PostMapping("/{id}/campos")
    public String salvarCampos(@PathVariable Long id,
                               @RequestParam String titulo,
                               @RequestParam String descricaoCurta,
                               @RequestParam String descricaoComplementar,
                               Model model) {
        Product produto = produto(id);
        pipeline.salvarCampos(produto, titulo, descricaoCurta, descricaoComplementar);
        return painel(model, produto);
    }

    @PostMapping("/{id}/aprovar")
    public String aprovar(@PathVariable Long id, Model model) {
        Product produto = produto(id);
        pipeline.aprovar(produto);
        return painel(model, produto);
    }

    @PostMapping("/{id}/publicar")
    public String publicar(@PathVariable Long id, Model model) {
        Product produto = produto(id);
        pipeline.publicar(produto);
        return painel(model, produto);
    }

    private void adicionarImagens(Product produto, List<MultipartFile> imagens) throws IOException {
        if (imagens == null) {
            return;
        }
        for (MultipartFile arquivo : imagens) {
            if (arquivo != null && !arquivo.isEmpty()) {
                pipeline.adicionarImagem(produto, arquivo.getBytes());
            }
        }
    }

    private Product produto(Long id) {
        return repo.findById(id).orElseThrow();
    }

    private String painel(Model model, Product produto) {
        preencher(model, produto);
        return FRAGMENTO_CONTEUDO;
    }

    private void preencher(Model model, Product produto) {
        model.addAttribute("p", produto);
        model.addAttribute("blingConectado", bling.conectado());
    }
}

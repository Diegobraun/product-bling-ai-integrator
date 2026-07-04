package com.loja.catalogbling.web;

import com.loja.catalogbling.ProductPipelineService;
import com.loja.catalogbling.bling.BlingAuthService;
import com.loja.catalogbling.domain.Product;
import com.loja.catalogbling.imagem.ImageProcessingService;
import com.loja.catalogbling.repository.ProductRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;

@Controller
@RequestMapping("/produtos")
public class ProductController {

    private static final String FRAGMENTO_CONTEUDO = "produtos/fragmentos/conteudo :: painel";
    private static final String FRAGMENTO_IMAGEM = "produtos/fragmentos/imagem :: bloco";

    private final ProductRepository repo;
    private final ProductPipelineService pipeline;
    private final BlingAuthService bling;

    public ProductController(ProductRepository repo, ProductPipelineService pipeline, BlingAuthService bling) {
        this.repo = repo;
        this.pipeline = pipeline;
        this.bling = bling;
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
    public String pesquisar(@RequestParam String nome) {
        Product produto = pipeline.criarPorPesquisa(nome.trim());
        return "redirect:/produtos/" + produto.getId();
    }

    @PostMapping
    public String criar(@RequestParam String dadosBrutos,
                        @RequestParam(required = false) String marca,
                        @RequestParam(required = false) String modelo,
                        @RequestParam(required = false) String categoria,
                        @RequestParam(required = false) String ean,
                        @RequestParam(required = false) String preco,
                        @RequestParam(required = false) MultipartFile imagem) throws IOException {
        Product produto = new Product();
        produto.setDadosBrutos(dadosBrutos);
        produto.setMarca(marca);
        produto.setModelo(modelo);
        produto.setCategoria(categoria);
        produto.setEan(ean);
        if (preco != null && !preco.isBlank()) {
            produto.setPreco(new BigDecimal(preco.replace(",", ".")));
        }
        repo.save(produto);

        if (imagem != null && !imagem.isEmpty()) {
            pipeline.processarImagem(produto, imagem.getBytes());
        }
        return "redirect:/produtos/" + produto.getId();
    }

    @GetMapping("/{id}")
    public String detalhe(@PathVariable Long id, Model model) {
        preencher(model, produto(id));
        return "produtos/detalhe";
    }

    @PostMapping("/{id}/imagem")
    public String enviarImagem(@PathVariable Long id, @RequestParam MultipartFile imagem, Model model) throws IOException {
        Product produto = produto(id);
        ImageProcessingService.Resultado resultado = pipeline.processarImagem(produto, imagem.getBytes());
        model.addAttribute("p", produto);
        model.addAttribute("resultadoImagem", resultado);
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

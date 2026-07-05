package com.loja.catalogbling.catalogo.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "produtos")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProductStatus status = ProductStatus.RASCUNHO;

    @Column(columnDefinition = "text")
    private String dadosBrutos;
    private String marca;
    private String modelo;
    private String categoria;
    private String ean;
    private String sku;
    private String fornecedor;
    private String fabricante;
    private BigDecimal preco;

    @Column(length = 500)
    private String titulo;
    @Column(columnDefinition = "text")
    private String descricaoCurta;
    @Column(columnDefinition = "text")
    private String descricaoComplementar;
    @Column(columnDefinition = "text")
    private String avaliacaoImagem;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("ordem ASC")
    private List<ProductImage> imagens = new ArrayList<>();

    private String blingProductId;
    @Column(columnDefinition = "text")
    private String erroPublicacao;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<ConversationTurn> conversa = new ArrayList<>();

    private Instant criadoEm = Instant.now();
    private Instant atualizadoEm = Instant.now();

    public void transicionarPara(ProductStatus destino) {
        this.status.exigirTransicao(destino);
        this.status = destino;
        this.atualizadoEm = Instant.now();
    }

    public void adicionarTurno(String papel, String conteudo) {
        this.conversa.add(new ConversationTurn(this, papel, conteudo));
    }

    public void adicionarImagem(ProductImage imagem) {
        this.imagens.add(imagem);
    }

    public boolean removerImagem(Long imagemId) {
        return this.imagens.removeIf(img -> imagemId.equals(img.getId()));
    }

    public ProductImage getImagemPrincipal() {
        return imagens.isEmpty() ? null : imagens.get(0);
    }

    public boolean temImagem() {
        return !imagens.isEmpty();
    }

    public int proximaOrdem() {
        return imagens.stream().mapToInt(ProductImage::getOrdem).max().orElse(-1) + 1;
    }

    public String getResumo() {
        return java.util.stream.Stream.of(marca, modelo, categoria, sku)
                .filter(v -> v != null && !v.isBlank())
                .collect(java.util.stream.Collectors.joining(" · "));
    }

    public Long getId() { return id; }
    public ProductStatus getStatus() { return status; }
    public void setStatus(ProductStatus status) { this.status = status; }
    public String getDadosBrutos() { return dadosBrutos; }
    public void setDadosBrutos(String v) { this.dadosBrutos = v; }
    public String getMarca() { return marca; }
    public void setMarca(String v) { this.marca = v; }
    public String getModelo() { return modelo; }
    public void setModelo(String v) { this.modelo = v; }
    public String getCategoria() { return categoria; }
    public void setCategoria(String v) { this.categoria = v; }
    public String getEan() { return ean; }
    public void setEan(String v) { this.ean = v; }
    public String getSku() { return sku; }
    public void setSku(String v) { this.sku = v; }
    public String getFornecedor() { return fornecedor; }
    public void setFornecedor(String v) { this.fornecedor = v; }
    public String getFabricante() { return fabricante; }
    public void setFabricante(String v) { this.fabricante = v; }
    public BigDecimal getPreco() { return preco; }
    public void setPreco(BigDecimal v) { this.preco = v; }
    public String getTitulo() { return titulo; }
    public void setTitulo(String v) { this.titulo = v; }
    public String getDescricaoCurta() { return descricaoCurta; }
    public void setDescricaoCurta(String v) { this.descricaoCurta = v; }
    public String getDescricaoComplementar() { return descricaoComplementar; }
    public void setDescricaoComplementar(String v) { this.descricaoComplementar = v; }
    public String getAvaliacaoImagem() { return avaliacaoImagem; }
    public void setAvaliacaoImagem(String v) { this.avaliacaoImagem = v; }
    public List<ProductImage> getImagens() { return imagens; }
    public String getBlingProductId() { return blingProductId; }
    public void setBlingProductId(String v) { this.blingProductId = v; }
    public String getErroPublicacao() { return erroPublicacao; }
    public void setErroPublicacao(String v) { this.erroPublicacao = v; }
    public List<ConversationTurn> getConversa() { return conversa; }
    public Instant getCriadoEm() { return criadoEm; }
}

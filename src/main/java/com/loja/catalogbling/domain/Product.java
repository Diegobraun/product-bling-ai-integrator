package com.loja.catalogbling.domain;

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
    private BigDecimal preco;

    @Column(length = 500)
    private String titulo;
    @Column(columnDefinition = "text")
    private String descricaoCurta;
    @Column(columnDefinition = "text")
    private String descricaoComplementar;
    @Column(columnDefinition = "text")
    private String avaliacaoImagem;

    private String imagemUrlPublica;
    private Integer imagemKb;
    private Boolean imagemAprovada;
    private Double imagemNitidez;

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
    public String getImagemUrlPublica() { return imagemUrlPublica; }
    public void setImagemUrlPublica(String v) { this.imagemUrlPublica = v; }
    public Integer getImagemKb() { return imagemKb; }
    public void setImagemKb(Integer v) { this.imagemKb = v; }
    public Boolean getImagemAprovada() { return imagemAprovada; }
    public void setImagemAprovada(Boolean v) { this.imagemAprovada = v; }
    public Double getImagemNitidez() { return imagemNitidez; }
    public void setImagemNitidez(Double v) { this.imagemNitidez = v; }
    public String getBlingProductId() { return blingProductId; }
    public void setBlingProductId(String v) { this.blingProductId = v; }
    public String getErroPublicacao() { return erroPublicacao; }
    public void setErroPublicacao(String v) { this.erroPublicacao = v; }
    public List<ConversationTurn> getConversa() { return conversa; }
    public Instant getCriadoEm() { return criadoEm; }
}

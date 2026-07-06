package com.loja.productbling.catalogo.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "produto_imagens")
public class ProductImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(length = 1000)
    private String urlPublica;
    private Integer kb;
    private Boolean aprovada;
    private int ordem;

    protected ProductImage() {
    }

    public ProductImage(Product product, String urlPublica, Integer kb, Boolean aprovada, int ordem) {
        this.product = product;
        this.urlPublica = urlPublica;
        this.kb = kb;
        this.aprovada = aprovada;
        this.ordem = ordem;
    }

    public String nomeArquivo() {
        if (urlPublica == null) {
            return null;
        }
        int barra = urlPublica.lastIndexOf('/');
        return barra >= 0 ? urlPublica.substring(barra + 1) : urlPublica;
    }

    public Long getId() { return id; }
    public String getUrlPublica() { return urlPublica; }
    public Integer getKb() { return kb; }
    public Boolean getAprovada() { return aprovada; }
    public int getOrdem() { return ordem; }
}

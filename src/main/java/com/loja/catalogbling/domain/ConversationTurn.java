package com.loja.catalogbling.domain;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "conversa_turnos")
public class ConversationTurn {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "produto_id")
    private Product product;

    private String papel;

    @Column(columnDefinition = "text")
    private String conteudo;

    private Instant criadoEm = Instant.now();

    protected ConversationTurn() {
    }

    public ConversationTurn(Product product, String papel, String conteudo) {
        this.product = product;
        this.papel = papel;
        this.conteudo = conteudo;
    }

    public Long getId() { return id; }
    public String getPapel() { return papel; }
    public String getConteudo() { return conteudo; }
}

package com.loja.catalogbling.catalogo.domain;

import java.util.Map;
import java.util.Set;

public enum ProductStatus {

    RASCUNHO("Rascunho"),
    GERADO("Gerado"),
    EM_REVISAO("Em revisão"),
    APROVADO("Aprovado"),
    PUBLICADO("Publicado"),
    ERRO_PUBLICACAO("Erro na publicação");

    private final String rotulo;

    ProductStatus(String rotulo) {
        this.rotulo = rotulo;
    }

    public String getRotulo() {
        return rotulo;
    }

    private static final Map<ProductStatus, Set<ProductStatus>> TRANSICOES = Map.of(
            RASCUNHO,        Set.of(GERADO),
            GERADO,          Set.of(EM_REVISAO, APROVADO),
            EM_REVISAO,      Set.of(EM_REVISAO, APROVADO, GERADO),
            APROVADO,        Set.of(EM_REVISAO, PUBLICADO, ERRO_PUBLICACAO),
            PUBLICADO,       Set.of(EM_REVISAO, PUBLICADO, ERRO_PUBLICACAO),
            ERRO_PUBLICACAO, Set.of(APROVADO, EM_REVISAO, PUBLICADO, ERRO_PUBLICACAO)
    );

    public boolean podeIrPara(ProductStatus destino) {
        return TRANSICOES.getOrDefault(this, Set.of()).contains(destino);
    }

    public void exigirTransicao(ProductStatus destino) {
        if (!podeIrPara(destino)) {
            throw new IllegalStateException("Transição inválida: %s -> %s".formatted(this, destino));
        }
    }
}

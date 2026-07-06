package com.loja.productbling.catalogo.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

import static com.loja.productbling.catalogo.domain.ProductStatus.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;

class ProductStatusTest {

    private static final Map<ProductStatus, Set<ProductStatus>> ESPERADO = new EnumMap<>(ProductStatus.class);

    static {
        ESPERADO.put(RASCUNHO, Set.of(GERADO));
        ESPERADO.put(GERADO, Set.of(EM_REVISAO, APROVADO));
        ESPERADO.put(EM_REVISAO, Set.of(EM_REVISAO, APROVADO, GERADO));
        ESPERADO.put(APROVADO, Set.of(EM_REVISAO, PUBLICADO, ERRO_PUBLICACAO));
        ESPERADO.put(PUBLICADO, Set.of(EM_REVISAO, PUBLICADO, ERRO_PUBLICACAO));
        ESPERADO.put(ERRO_PUBLICACAO, Set.of(APROVADO, EM_REVISAO, PUBLICADO, ERRO_PUBLICACAO));
    }

    @ParameterizedTest
    @EnumSource(ProductStatus.class)
    void matrizCompletaDeTransicoes(ProductStatus origem) {
        for (ProductStatus destino : ProductStatus.values()) {
            boolean permitido = ESPERADO.get(origem).contains(destino);
            assertThat(origem.podeIrPara(destino))
                    .as("%s -> %s", origem, destino)
                    .isEqualTo(permitido);
        }
    }

    @Test
    void exigirTransicaoValidaNaoLanca() {
        assertThatCode(() -> RASCUNHO.exigirTransicao(GERADO)).doesNotThrowAnyException();
    }

    @Test
    void exigirTransicaoInvalidaLanca() {
        assertThatThrownBy(() -> RASCUNHO.exigirTransicao(PUBLICADO))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Transição inválida");
    }

    @Test
    void rotuloLegivel() {
        assertThat(ERRO_PUBLICACAO.getRotulo()).isEqualTo("Erro na publicação");
    }
}

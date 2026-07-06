package com.loja.productbling.catalogo.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CorTest {

    @Test
    void reconheceCorEmPortugues() {
        Cor cor = Cor.deTexto("mouse rosa sem fio");
        assertThat(cor.presente()).isTrue();
        assertThat(cor.valor()).isEqualTo("rosa");
    }

    @Test
    void traduzSinonimoEmIngles() {
        assertThat(Cor.deTexto("wireless black mouse").valor()).isEqualTo("preto");
        assertThat(Cor.deTexto("rose gold").valor()).isEqualTo("rosa");
    }

    @Test
    void semCorFicaAusente() {
        Cor cor = Cor.deTexto("mouse sem fio");
        assertThat(cor.presente()).isFalse();
        assertThat(cor.valor()).isEmpty();
        assertThat(cor.tokensParaExcluir()).isEmpty();
    }

    @Test
    void textoNuloEhAusente() {
        assertThat(Cor.deTexto(null).presente()).isFalse();
    }

    @Test
    void tokensParaExcluirTrazemAsOutrasCoresMasNaoAAtual() {
        Cor rosa = Cor.deTexto("rosa");
        assertThat(rosa.tokensParaExcluir())
                .contains("preto", "black", "azul", "white")
                .doesNotContain("rosa", "rose", "pink");
    }
}

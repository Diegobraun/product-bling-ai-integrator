package com.loja.productbling.catalogo.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EntradaDeBuscaTest {

    @Test
    void detectaLink() {
        assertThat(EntradaDeBusca.de("https://loja.com/p/1").ehLink()).isTrue();
        assertThat(EntradaDeBusca.de("mouse logitech").ehLink()).isFalse();
    }

    @Test
    void extraiUrlIgnorandoTextoAoRedor() {
        EntradaDeBusca e = EntradaDeBusca.de("https://loja.com/p/1 Rosa");
        assertThat(e.url()).isEqualTo("https://loja.com/p/1");
    }

    @Test
    void semLinkUrlEhNula() {
        assertThat(EntradaDeBusca.de("mouse rosa").url()).isNull();
    }

    @Test
    void textoDigitadoRemoveOsTokensDeUrl() {
        EntradaDeBusca e = EntradaDeBusca.de("https://loja.com/p/1 mouse rosa");
        assertThat(e.textoDigitado()).isEqualTo("mouse rosa");
    }

    @Test
    void corVemDoTextoDigitadoMesmoComLink() {
        Cor cor = EntradaDeBusca.de("https://loja.com/p/1 Rosa").cor();
        assertThat(cor.valor()).isEqualTo("rosa");
    }

    @Test
    void corCaiParaOTextoInteiroQuandoNaoHaDigitado() {
        Cor cor = EntradaDeBusca.de("https://loja.com/rose-mouse").cor();
        assertThat(cor.valor()).isEqualTo("rosa");
    }
}

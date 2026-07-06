package com.loja.productbling.ia.application;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RespostaJsonTest {

    @Test
    void removeCercasDeMarkdown() {
        String bruto = "```json\n{\"a\":1}\n```";
        assertThat(RespostaJson.extrairObjeto(bruto)).isEqualTo("{\"a\":1}");
    }

    @Test
    void removeCercaSemLinguagem() {
        String bruto = "```\n{\"a\":1}\n```";
        assertThat(RespostaJson.extrairObjeto(bruto)).isEqualTo("{\"a\":1}");
    }

    @Test
    void fatiaTextoAntesEDepoisDasChaves() {
        String bruto = "Claro! Aqui vai:\n{\"titulo\":\"x\"}\nEspero ter ajudado.";
        assertThat(RespostaJson.extrairObjeto(bruto)).isEqualTo("{\"titulo\":\"x\"}");
    }

    @Test
    void mantemObjetoAninhado() {
        String bruto = "{\"a\":{\"b\":1}}";
        assertThat(RespostaJson.extrairObjeto(bruto)).isEqualTo("{\"a\":{\"b\":1}}");
    }

    @Test
    void semJsonRetornaTextoLimpo() {
        assertThat(RespostaJson.extrairObjeto("  sem json aqui  ")).isEqualTo("sem json aqui");
    }
}

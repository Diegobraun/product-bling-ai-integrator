package com.loja.productbling.ia.application;

import com.loja.productbling.catalogo.domain.Product;
import com.loja.productbling.ia.domain.ChatIa;
import com.loja.productbling.ia.domain.ConteudoGerado;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ConteudoIaServiceTest {

    private final ChatIa chat = mock(ChatIa.class);
    private final ConteudoIaService service = new ConteudoIaService(chat);

    @Test
    void gerarParseiaOJsonDaIa() {
        String json = """
            {"titulo":"Mouse X","descricaoComplementar":"<p>Longa</p>",
             "descricaoCurta":"Curta","avaliacaoImagem":{"adequada":true,"observacoes":"boa"}}
            """;
        when(chat.completar(anyString(), anyList(), any(), anyInt())).thenReturn(json);

        ConteudoGerado gerado = service.gerar(produto(), null, "image/jpeg");

        assertThat(gerado.titulo()).isEqualTo("Mouse X");
        assertThat(gerado.descricaoComplementar()).isEqualTo("<p>Longa</p>");
        assertThat(gerado.descricaoCurta()).isEqualTo("Curta");
        assertThat(gerado.avaliacaoImagem()).isEqualTo("boa");
        assertThat(gerado.respostaBruta()).isEqualTo(json);
    }

    @Test
    void aceitaJsonComCercasEText() {
        when(chat.completar(anyString(), anyList(), any(), anyInt()))
                .thenReturn("Segue:\n```json\n{\"titulo\":\"T\"}\n```");
        assertThat(service.gerar(produto(), null, "image/jpeg").titulo()).isEqualTo("T");
    }

    @Test
    void respostaNaoJsonLanca() {
        when(chat.completar(anyString(), anyList(), any(), anyInt()))
                .thenReturn("desculpe, não consegui");
        assertThatThrownBy(() -> service.gerar(produto(), null, "image/jpeg"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("parsear");
    }

    @Test
    void revisarUsaOHistoricoDaConversa() {
        Product p = produto();
        p.adicionarTurno("user", "gera aí");
        p.adicionarTurno("assistant", "{\"titulo\":\"velho\"}");
        when(chat.completar(anyString(), anyList(), any(), anyInt()))
                .thenReturn("{\"titulo\":\"novo\"}");

        assertThat(service.revisar(p, "encurte o título").titulo()).isEqualTo("novo");
    }

    private Product produto() {
        Product p = new Product();
        p.setDadosBrutos("mouse sem fio");
        p.setMarca("Logitech");
        return p;
    }
}

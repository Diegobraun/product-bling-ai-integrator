package com.loja.productbling.ia.application;

import com.loja.productbling.ia.application.VerificacaoImagemIaService.Classificacao;
import com.loja.productbling.ia.domain.ChatIa;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class VerificacaoImagemIaServiceTest {

    private final ChatIa chat = mock(ChatIa.class);
    private final VerificacaoImagemIaService service = new VerificacaoImagemIaService(chat);

    private Classificacao classificar() {
        return service.classificar("mouse rosa", "rosa", "base64==", "image/jpeg");
    }

    @Test
    void descricaoVaziaNaoChamaIaERetornaLimpa() {
        assertThat(service.classificar("  ", "rosa", "b64", "image/jpeg")).isEqualTo(Classificacao.LIMPA);
        verify(chat, never()).completarVolume(anyString(), anyList(), any(), anyInt());
    }

    @Test
    void tipoRejeitarMapeiaParaRejeitar() {
        when(chat.completarVolume(anyString(), anyList(), any(), anyInt()))
                .thenReturn("{\"tipo\":\"rejeitar\",\"motivo\":\"banner\"}");
        assertThat(classificar()).isEqualTo(Classificacao.REJEITAR);
    }

    @Test
    void tipoAmbientadaMapeiaParaAmbientada() {
        when(chat.completarVolume(anyString(), anyList(), any(), anyInt()))
                .thenReturn("{\"tipo\":\"ambientada\"}");
        assertThat(classificar()).isEqualTo(Classificacao.AMBIENTADA);
    }

    @Test
    void tipoDesconhecidoOuLimpaCaiEmLimpa() {
        when(chat.completarVolume(anyString(), anyList(), any(), anyInt()))
                .thenReturn("{\"tipo\":\"qualquer\"}");
        assertThat(classificar()).isEqualTo(Classificacao.LIMPA);
    }

    @Test
    void falhaDaIaEhToleradaComoLimpa() {
        when(chat.completarVolume(anyString(), anyList(), any(), anyInt()))
                .thenThrow(new RuntimeException("timeout"));
        assertThat(classificar()).isEqualTo(Classificacao.LIMPA);
    }
}

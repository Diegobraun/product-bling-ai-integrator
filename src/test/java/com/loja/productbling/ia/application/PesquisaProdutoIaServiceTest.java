package com.loja.productbling.ia.application;

import com.loja.productbling.ia.domain.PesquisaDeProduto;
import com.loja.productbling.ia.domain.PesquisaEntrada;
import com.loja.productbling.ia.domain.PesquisaWebIa;
import com.loja.productbling.support.FakeObjectProvider;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PesquisaProdutoIaServiceTest {

    private PesquisaProdutoIaService comProvedor(PesquisaWebIa ia) {
        return new PesquisaProdutoIaService(new FakeObjectProvider<>(ia));
    }

    private PesquisaEntrada entrada() {
        return new PesquisaEntrada("mouse rosa", null, "Logitech", "M350", null, null, null, null);
    }

    @Test
    void semProvedorLancaMensagemClara() {
        PesquisaProdutoIaService service = comProvedor(null);
        assertThatThrownBy(() -> service.pesquisar(entrada()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("pesquisa web");
    }

    @Test
    void parseiaResultadoEFiltraUrlsNaoHttp() {
        PesquisaWebIa ia = mock(PesquisaWebIa.class);
        PesquisaProdutoIaService service = comProvedor(ia);
        when(ia.pesquisar(anyString(), anyString(), anyInt())).thenReturn("""
            {"marca":"Logitech","modelo":"M350","categoria":"mouse","ean":"789",
             "dadosBrutos":"ficha",
             "imagens":["https://cdn/x.jpg","ftp://ignora","javascript:void"],
             "paginas":["https://loja/p/1","   "]}
            """);

        PesquisaDeProduto r = service.pesquisar(entrada());

        assertThat(r.marca()).isEqualTo("Logitech");
        assertThat(r.categoria()).isEqualTo("mouse");
        assertThat(r.imagens()).containsExactly("https://cdn/x.jpg");
        assertThat(r.paginas()).containsExactly("https://loja/p/1");
    }

    @Test
    void tratantoNullTextualComoAusente() {
        PesquisaWebIa ia = mock(PesquisaWebIa.class);
        PesquisaProdutoIaService service = comProvedor(ia);
        when(ia.pesquisar(anyString(), anyString(), anyInt()))
                .thenReturn("{\"marca\":\"null\",\"ean\":null,\"imagens\":[],\"paginas\":[]}");

        PesquisaDeProduto r = service.pesquisar(entrada());
        assertThat(r.marca()).isNull();
        assertThat(r.ean()).isNull();
        assertThat(r.imagens()).isEmpty();
    }
}

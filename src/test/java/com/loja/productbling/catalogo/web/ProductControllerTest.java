package com.loja.productbling.catalogo.web;

import com.loja.productbling.bling.infrastructure.BlingAuthService;
import com.loja.productbling.bling.infrastructure.BlingProductClient;
import com.loja.productbling.catalogo.application.ProductPipelineService;
import com.loja.productbling.catalogo.domain.Product;
import com.loja.productbling.ia.domain.PesquisaEntrada;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductController.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductPipelineService pipeline;
    @MockBean
    private com.loja.productbling.catalogo.domain.ProductRepository repo;
    @MockBean
    private BlingAuthService bling;
    @MockBean
    private BlingProductClient blingProdutos;

    private Product comId(long id) throws Exception {
        Product p = new Product();
        var f = Product.class.getDeclaredField("id");
        f.setAccessible(true);
        f.set(p, id);
        return p;
    }

    @Test
    void pesquisarCriaProdutoERedireciona() throws Exception {
        when(pipeline.criarPorPesquisa(any())).thenReturn(comId(7L));

        mockMvc.perform(post("/produtos/pesquisar")
                        .param("nome", "  mouse rosa  ")
                        .param("marca", "Logitech"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/produtos/7"));

        ArgumentCaptor<PesquisaEntrada> captor = ArgumentCaptor.forClass(PesquisaEntrada.class);
        verify(pipeline).criarPorPesquisa(captor.capture());
        assertThat(captor.getValue().nome()).isEqualTo("mouse rosa");
        assertThat(captor.getValue().marca()).isEqualTo("Logitech");
    }

    @Test
    void importarDoBlingRedireciona() throws Exception {
        when(pipeline.importarDoBling("42")).thenReturn(comId(7L));

        mockMvc.perform(post("/produtos/bling/importar").param("blingId", " 42 "))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/produtos/7"));

        verify(pipeline).importarDoBling("42");
    }

    @Test
    void criarManualSalvaERedireciona() throws Exception {
        mockMvc.perform(post("/produtos")
                        .param("dadosBrutos", "ficha técnica")
                        .param("marca", "Logitech"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/produtos/*"));

        verify(repo).save(any(Product.class));
    }
}

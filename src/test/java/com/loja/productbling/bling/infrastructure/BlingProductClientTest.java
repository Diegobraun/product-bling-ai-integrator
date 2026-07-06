package com.loja.productbling.bling.infrastructure;

import com.loja.productbling.catalogo.domain.Product;
import com.loja.productbling.config.BlingProperties;
import com.loja.productbling.ia.infrastructure.IaHttp;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class BlingProductClientTest {

    private MockWebServer server;
    private BlingProductClient client;

    @BeforeEach
    void setUp() throws Exception {
        server = new MockWebServer();
        server.start();
        BlingProperties props = new BlingProperties("id", "secret", server.url("/Api/v3").toString());
        BlingAuthService auth = new BlingAuthService(null, null, props) {
            @Override
            public String accessTokenValido() {
                return "tok";
            }
        };
        client = new BlingProductClient(IaHttp.clientePadrao(), props, auth);
    }

    @AfterEach
    void tearDown() throws Exception {
        server.shutdown();
    }

    private MockResponse json(String corpo) {
        return new MockResponse().setBody(corpo).addHeader("Content-Type", "application/json");
    }

    @Test
    void criarProdutoEnviaTokenERetornaId() throws Exception {
        server.enqueue(json("{\"data\":{\"id\":123}}"));
        Product p = new Product();
        p.setTitulo("Mouse X");

        String id = client.criarProduto(p);

        assertThat(id).isEqualTo("123");
        RecordedRequest req = server.takeRequest();
        assertThat(req.getMethod()).isEqualTo("POST");
        assertThat(req.getPath()).endsWith("/produtos");
        assertThat(req.getHeader("Authorization")).isEqualTo("Bearer tok");
    }

    @Test
    void obterProdutoDesembrulhaCampoData() {
        server.enqueue(json("{\"data\":{\"id\":9,\"nome\":\"Y\"}}"));
        Map<String, Object> dados = client.obterProduto("9");
        assertThat(dados.get("nome")).isEqualTo("Y");
    }

    @Test
    void buscarProdutoExistentePriorizaGtin() {
        server.enqueue(json("{\"data\":[{\"id\":5,\"gtin\":\"789\",\"nome\":\"outro\"}]}"));
        Product p = new Product();
        p.setTitulo("Mouse X");
        p.setEan("789");

        assertThat(client.buscarProdutoExistente(p)).isEqualTo("5");
    }
}

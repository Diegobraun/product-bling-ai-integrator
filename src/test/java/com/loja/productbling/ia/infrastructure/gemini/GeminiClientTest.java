package com.loja.productbling.ia.infrastructure.gemini;

import com.fasterxml.jackson.databind.JsonNode;
import com.loja.productbling.config.GeminiProperties;
import com.loja.productbling.ia.infrastructure.IaHttp;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GeminiClientTest {

    private MockWebServer server;

    @BeforeEach
    void setUp() throws Exception {
        server = new MockWebServer();
        server.start();
    }

    @AfterEach
    void tearDown() throws Exception {
        server.shutdown();
    }

    private GeminiClient client(String modelo, List<String> fallbacks) {
        GeminiProperties props = new GeminiProperties(
                "key", modelo, fallbacks, List.of(), server.url("/").toString());
        return new GeminiClient(IaHttp.clientePadrao(), props);
    }

    @Test
    void caiParaOProximoModeloEmCasoDe429() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(429));
        server.enqueue(new MockResponse()
                .setBody("{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"oi\"}]}}]}")
                .addHeader("Content-Type", "application/json"));

        GeminiClient client = client("m1", List.of("m2"));
        JsonNode resposta = client.gerarConteudo(Map.of("x", 1));

        assertThat(server.getRequestCount()).isEqualTo(2);
        RecordedRequest primeira = server.takeRequest();
        RecordedRequest segunda = server.takeRequest();
        assertThat(primeira.getPath()).contains("m1:generateContent");
        assertThat(segunda.getPath()).contains("m2:generateContent");
        assertThat(client.extrairTexto(resposta)).isEqualTo("oi");
    }

    @Test
    void enviaChaveNoHeader() throws Exception {
        server.enqueue(new MockResponse()
                .setBody("{\"candidates\":[]}").addHeader("Content-Type", "application/json"));
        client("m1", List.of()).gerarConteudo(Map.of("x", 1));
        assertThat(server.takeRequest().getHeader("x-goog-api-key")).isEqualTo("key");
    }
}

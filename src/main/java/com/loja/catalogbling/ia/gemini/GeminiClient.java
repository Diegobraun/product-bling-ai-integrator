package com.loja.catalogbling.ia.gemini;

import com.fasterxml.jackson.databind.JsonNode;
import com.loja.catalogbling.config.GeminiProperties;
import com.loja.catalogbling.ia.IaHttp;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Component
public class GeminiClient {

    private static final String BASE = "https://generativelanguage.googleapis.com/v1beta/models/";

    private final RestClient http = IaHttp.clientePadrao();
    private final GeminiProperties props;

    public GeminiClient(GeminiProperties props) {
        this.props = props;
    }

    public JsonNode gerarConteudo(Map<String, Object> corpo) {
        return http.post()
                .uri(BASE + props.model() + ":generateContent")
                .header("x-goog-api-key", props.apiKey())
                .header("Content-Type", "application/json")
                .body(corpo)
                .retrieve()
                .body(JsonNode.class);
    }

    public String extrairTexto(JsonNode resposta) {
        StringBuilder texto = new StringBuilder();
        JsonNode parts = resposta == null
                ? null : resposta.path("candidates").path(0).path("content").path("parts");
        if (parts != null && parts.isArray()) {
            for (JsonNode parte : parts) {
                if (parte.has("text")) {
                    texto.append(parte.path("text").asText());
                }
            }
        }
        return texto.toString().trim();
    }
}

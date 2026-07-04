package com.loja.catalogbling.ia.claude;

import com.fasterxml.jackson.databind.JsonNode;
import com.loja.catalogbling.config.AnthropicProperties;
import com.loja.catalogbling.ia.IaHttp;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Component
public class AnthropicMessagesClient {

    private static final String ENDPOINT = "https://api.anthropic.com/v1/messages";
    private static final String VERSAO_API = "2023-06-01";

    private final RestClient http = IaHttp.clientePadrao();
    private final AnthropicProperties props;

    public AnthropicMessagesClient(AnthropicProperties props) {
        this.props = props;
    }

    public String modelo() {
        return props.model();
    }

    public JsonNode enviar(Map<String, Object> corpo) {
        return http.post()
                .uri(ENDPOINT)
                .header("x-api-key", props.apiKey())
                .header("anthropic-version", VERSAO_API)
                .header("Content-Type", "application/json")
                .body(corpo)
                .retrieve()
                .body(JsonNode.class);
    }

    public String extrairTexto(JsonNode resposta) {
        StringBuilder texto = new StringBuilder();
        JsonNode content = resposta == null ? null : resposta.path("content");
        if (content != null && content.isArray()) {
            for (JsonNode bloco : content) {
                if ("text".equals(bloco.path("type").asText())) {
                    texto.append(bloco.path("text").asText());
                }
            }
        }
        return texto.toString().trim();
    }
}

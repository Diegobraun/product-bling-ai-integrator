package com.loja.catalogbling.claude;

import com.fasterxml.jackson.databind.JsonNode;
import com.loja.catalogbling.config.AnthropicProperties;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Component
public class AnthropicMessagesClient {

    private static final String ENDPOINT = "https://api.anthropic.com/v1/messages";
    private static final String VERSAO_API = "2023-06-01";
    private static final int TIMEOUT_CONEXAO_MS = 15_000;
    private static final int TIMEOUT_LEITURA_MS = 300_000;

    private final RestClient http;
    private final AnthropicProperties props;

    public AnthropicMessagesClient(AnthropicProperties props) {
        this.props = props;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(TIMEOUT_CONEXAO_MS);
        factory.setReadTimeout(TIMEOUT_LEITURA_MS);
        this.http = RestClient.builder().requestFactory(factory).build();
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

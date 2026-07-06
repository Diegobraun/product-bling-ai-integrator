package com.loja.productbling.ia.infrastructure.gemini;

import com.fasterxml.jackson.databind.JsonNode;
import com.loja.productbling.config.GeminiProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class GeminiClient {

    private static final Logger log = LoggerFactory.getLogger(GeminiClient.class);

    private final RestClient http;
    private final GeminiProperties props;

    public GeminiClient(RestClient http, GeminiProperties props) {
        this.http = http;
        this.props = props;
    }

    public JsonNode gerarConteudo(Map<String, Object> corpo) {
        return chamar(corpo, modelosEmOrdem(props.model(), props.fallbackModels()));
    }

    public JsonNode gerarConteudoVerificacao(Map<String, Object> corpo) {
        List<String> modelos = modelosEmOrdem(null, props.verificacaoModels());
        return chamar(corpo, modelos.isEmpty() ? modelosEmOrdem(props.model(), props.fallbackModels()) : modelos);
    }

    private JsonNode chamar(Map<String, Object> corpo, List<String> modelos) {
        RestClientResponseException ultimoErro = null;

        for (String modelo : modelos) {
            try {
                return http.post()
                        .uri(props.baseUrl() + modelo + ":generateContent")
                        .header("x-goog-api-key", props.apiKey())
                        .header("Content-Type", "application/json")
                        .body(corpo)
                        .retrieve()
                        .body(JsonNode.class);
            } catch (RestClientResponseException e) {
                int status = e.getStatusCode().value();
                if (status == 429 || status == 503 || status == 500 || status == 404) {
                    log.warn("Modelo Gemini {} indisponível (HTTP {}); tentando o próximo modelo.",
                            modelo, status);
                    ultimoErro = e;
                } else {
                    throw e;
                }
            }
        }
        if (ultimoErro != null) {
            throw ultimoErro;
        }
        throw new IllegalStateException("Nenhum modelo Gemini configurado (gemini.model / gemini.fallback-models).");
    }

    private List<String> modelosEmOrdem(String principal, List<String> fallbacks) {
        List<String> modelos = new ArrayList<>();
        adicionar(modelos, principal);
        if (fallbacks != null) {
            for (String modelo : fallbacks) {
                adicionar(modelos, modelo);
            }
        }
        return modelos;
    }

    private void adicionar(List<String> modelos, String modelo) {
        if (modelo != null && !modelo.isBlank() && !modelos.contains(modelo.trim())) {
            modelos.add(modelo.trim());
        }
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

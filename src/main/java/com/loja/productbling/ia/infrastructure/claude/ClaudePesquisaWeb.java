package com.loja.productbling.ia.infrastructure.claude;

import com.fasterxml.jackson.databind.JsonNode;
import com.loja.productbling.ia.domain.PesquisaWebIa;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "ia.pesquisa-provider", havingValue = "claude", matchIfMissing = true)
public class ClaudePesquisaWeb implements PesquisaWebIa {

    private static final int MAX_CONTINUACOES = 5;
    private static final int MAX_USOS_POR_FERRAMENTA = 8;

    private final AnthropicMessagesClient api;

    public ClaudePesquisaWeb(AnthropicMessagesClient api) {
        this.api = api;
    }

    @Override
    public String pesquisar(String system, String prompt, int maxTokens) {
        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(Map.of("role", "user", "content", prompt));

        JsonNode resposta = null;
        for (int i = 0; i <= MAX_CONTINUACOES; i++) {
            resposta = api.enviar(corpo(system, messages, maxTokens));
            if (!"pause_turn".equals(resposta.path("stop_reason").asText())) {
                break;
            }
            messages.add(Map.of("role", "assistant", "content", resposta.path("content")));
        }
        return api.extrairTexto(resposta);
    }

    private Map<String, Object> corpo(String system, List<Map<String, Object>> messages, int maxTokens) {
        return Map.of(
                "model", api.modelo(),
                "max_tokens", maxTokens,
                "system", system,
                "messages", messages,
                "tools", List.of(
                        Map.of("type", "web_search_20260209", "name", "web_search",
                                "max_uses", MAX_USOS_POR_FERRAMENTA),
                        Map.of("type", "web_fetch_20260209", "name", "web_fetch",
                                "max_uses", MAX_USOS_POR_FERRAMENTA)));
    }
}

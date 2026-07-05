package com.loja.catalogbling.ia.infrastructure.gemini;

import com.loja.catalogbling.ia.domain.PesquisaWebIa;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "ia.pesquisa-provider", havingValue = "gemini")
public class GeminiPesquisaWeb implements PesquisaWebIa {

    private final GeminiClient api;

    public GeminiPesquisaWeb(GeminiClient api) {
        this.api = api;
    }

    @Override
    public String pesquisar(String system, String prompt, int maxTokens) {
        Map<String, Object> corpo = Map.of(
                "system_instruction", Map.of("parts", List.of(Map.of("text", system))),
                "contents", List.of(Map.of(
                        "role", "user",
                        "parts", List.of(Map.of("text", prompt)))),
                "tools", List.of(Map.of("google_search", Map.of())),
                "generation_config", Map.of("max_output_tokens", maxTokens));
        return api.extrairTexto(api.gerarConteudo(corpo));
    }
}

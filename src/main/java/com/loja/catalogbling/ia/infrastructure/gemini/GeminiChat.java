package com.loja.catalogbling.ia.infrastructure.gemini;

import com.loja.catalogbling.ia.domain.ChatIa;
import com.loja.catalogbling.ia.domain.MensagemIa;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "ia.provider", havingValue = "gemini")
public class GeminiChat implements ChatIa {

    private final GeminiClient api;

    public GeminiChat(GeminiClient api) {
        this.api = api;
    }

    @Override
    public String completar(String system, List<MensagemIa> mensagens, ImagemAnexa imagem, int maxTokens) {
        return api.extrairTexto(api.gerarConteudo(montarCorpo(system, mensagens, imagem, maxTokens)));
    }

    @Override
    public String completarVolume(String system, List<MensagemIa> mensagens, ImagemAnexa imagem, int maxTokens) {
        return api.extrairTexto(api.gerarConteudoVerificacao(montarCorpo(system, mensagens, imagem, maxTokens)));
    }

    private Map<String, Object> montarCorpo(String system, List<MensagemIa> mensagens,
                                            ImagemAnexa imagem, int maxTokens) {
        List<Map<String, Object>> contents = new ArrayList<>();
        for (int i = 0; i < mensagens.size(); i++) {
            MensagemIa mensagem = mensagens.get(i);
            boolean ultima = i == mensagens.size() - 1;
            contents.add(Map.of(
                    "role", papelGemini(mensagem.papel()),
                    "parts", ultima && imagem != null
                            ? partesComImagem(mensagem.conteudo(), imagem)
                            : List.of(Map.of("text", mensagem.conteudo()))));
        }

        return Map.of(
                "system_instruction", Map.of("parts", List.of(Map.of("text", system))),
                "contents", contents,
                "generation_config", Map.of(
                        "max_output_tokens", maxTokens,
                        "thinking_config", Map.of("thinking_budget", 0)));
    }

    private List<Map<String, Object>> partesComImagem(String texto, ImagemAnexa imagem) {
        return List.of(
                Map.of("text", texto),
                Map.of("inline_data", Map.of(
                        "mime_type", imagem.mediaType(),
                        "data", imagem.base64())));
    }

    private String papelGemini(String papel) {
        return MensagemIa.ASSISTENTE.equals(papel) ? "model" : "user";
    }
}

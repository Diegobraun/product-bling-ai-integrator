package com.loja.catalogbling.ia.claude;

import com.loja.catalogbling.ia.ChatIa;
import com.loja.catalogbling.ia.MensagemIa;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "ia.provider", havingValue = "claude", matchIfMissing = true)
public class ClaudeChat implements ChatIa {

    private final AnthropicMessagesClient api;

    public ClaudeChat(AnthropicMessagesClient api) {
        this.api = api;
    }

    @Override
    public String completar(String system, List<MensagemIa> mensagens, ImagemAnexa imagem, int maxTokens) {
        List<Map<String, Object>> messages = new ArrayList<>();
        for (int i = 0; i < mensagens.size(); i++) {
            MensagemIa mensagem = mensagens.get(i);
            boolean ultima = i == mensagens.size() - 1;
            messages.add(Map.of(
                    "role", mensagem.papel(),
                    "content", ultima && imagem != null
                            ? conteudoComImagem(mensagem.conteudo(), imagem)
                            : mensagem.conteudo()));
        }

        Map<String, Object> corpo = Map.of(
                "model", api.modelo(),
                "max_tokens", maxTokens,
                "system", system,
                "messages", messages);
        return api.extrairTexto(api.enviar(corpo));
    }

    private List<Map<String, Object>> conteudoComImagem(String texto, ImagemAnexa imagem) {
        return List.of(
                Map.of("type", "text", "text", texto),
                Map.of("type", "image", "source", Map.of(
                        "type", "base64",
                        "media_type", imagem.mediaType(),
                        "data", imagem.base64())));
    }
}

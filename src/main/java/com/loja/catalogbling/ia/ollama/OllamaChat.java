package com.loja.catalogbling.ia.ollama;

import com.fasterxml.jackson.databind.JsonNode;
import com.loja.catalogbling.config.OllamaProperties;
import com.loja.catalogbling.ia.ChatIa;
import com.loja.catalogbling.ia.IaHttp;
import com.loja.catalogbling.ia.MensagemIa;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "ia.provider", havingValue = "ollama")
public class OllamaChat implements ChatIa {

    private final RestClient http = IaHttp.clientePadrao();
    private final OllamaProperties props;

    public OllamaChat(OllamaProperties props) {
        this.props = props;
    }

    @Override
    public String completar(String system, List<MensagemIa> mensagens, ImagemAnexa imagem, int maxTokens) {
        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", system));
        for (int i = 0; i < mensagens.size(); i++) {
            MensagemIa mensagem = mensagens.get(i);
            boolean ultima = i == mensagens.size() - 1;
            Map<String, Object> message = new LinkedHashMap<>();
            message.put("role", mensagem.papel());
            message.put("content", mensagem.conteudo());
            if (ultima && imagem != null) {
                message.put("images", List.of(imagem.base64()));
            }
            messages.add(message);
        }

        Map<String, Object> corpo = Map.of(
                "model", props.model(),
                "messages", messages,
                "stream", false,
                "options", Map.of("num_predict", maxTokens));

        JsonNode resposta = http.post()
                .uri(props.baseUrl().replaceAll("/+$", "") + "/api/chat")
                .header("Content-Type", "application/json")
                .body(corpo)
                .retrieve()
                .body(JsonNode.class);

        return resposta == null ? "" : resposta.path("message").path("content").asText().trim();
    }
}

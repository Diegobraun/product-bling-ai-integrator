package com.loja.catalogbling.claude;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loja.catalogbling.domain.ConversationTurn;
import com.loja.catalogbling.domain.Product;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class ClaudeContentService {

    private static final int MAX_TOKENS = 4000;

    private final AnthropicMessagesClient api;
    private final ObjectMapper json = new ObjectMapper();

    public ClaudeContentService(AnthropicMessagesClient api) {
        this.api = api;
    }

    public record ConteudoGerado(String titulo, String descricaoComplementar,
                                 String descricaoCurta, String avaliacaoImagem,
                                 boolean imagemAdequada, String respostaBruta) {}

    public ConteudoGerado gerar(Product produto, String imagemBase64, String mediaType) {
        List<Map<String, Object>> conteudoUser = new ArrayList<>();
        conteudoUser.add(Map.of("type", "text",
                "text", ProductPrompts.primeiraMensagem(
                        produto.getDadosBrutos(), produto.getMarca(), produto.getModelo(),
                        produto.getCategoria(), produto.getEan())));

        if (imagemBase64 != null) {
            conteudoUser.add(Map.of(
                    "type", "image",
                    "source", Map.of(
                            "type", "base64",
                            "media_type", mediaType,
                            "data", imagemBase64)));
        }

        return chamar(List.of(Map.of("role", "user", "content", conteudoUser)));
    }

    public ConteudoGerado revisar(Product produto, String pedidoDeAjuste) {
        List<Map<String, Object>> messages = new ArrayList<>();
        for (ConversationTurn turno : produto.getConversa()) {
            messages.add(Map.of("role", turno.getPapel(), "content", turno.getConteudo()));
        }
        messages.add(Map.of("role", "user", "content", pedidoDeAjuste));
        return chamar(messages);
    }

    private ConteudoGerado chamar(List<Map<String, Object>> messages) {
        Map<String, Object> corpo = Map.of(
                "model", api.modelo(),
                "max_tokens", MAX_TOKENS,
                "system", ProductPrompts.SYSTEM,
                "messages", messages);
        return parsear(api.extrairTexto(api.enviar(corpo)));
    }

    private ConteudoGerado parsear(String texto) {
        try {
            JsonNode n = json.readTree(RespostaJson.extrairObjeto(texto));
            JsonNode img = n.path("avaliacaoImagem");
            return new ConteudoGerado(
                    n.path("titulo").asText(),
                    n.path("descricaoComplementar").asText(),
                    n.path("descricaoCurta").asText(),
                    img.path("observacoes").asText(""),
                    img.path("adequada").asBoolean(false),
                    texto);
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Não consegui parsear a resposta do Claude como JSON:\n" + texto, e);
        }
    }
}

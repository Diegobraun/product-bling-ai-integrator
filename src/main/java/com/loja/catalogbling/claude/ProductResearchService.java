package com.loja.catalogbling.claude;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class ProductResearchService {

    private static final int MAX_TOKENS = 8000;
    private static final int MAX_CONTINUACOES = 5;
    private static final int MAX_USOS_POR_FERRAMENTA = 8;

    private final AnthropicMessagesClient api;
    private final ObjectMapper json = new ObjectMapper();

    public ProductResearchService(AnthropicMessagesClient api) {
        this.api = api;
    }

    public record Pesquisa(String marca, String modelo, String categoria, String ean,
                           String dadosBrutos, List<String> imagens, String respostaBruta) {}

    public Pesquisa pesquisar(String nomeProduto) {
        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(Map.of("role", "user", "content", ProductPrompts.mensagemPesquisa(nomeProduto)));

        JsonNode resposta = null;
        for (int i = 0; i <= MAX_CONTINUACOES; i++) {
            resposta = api.enviar(corpo(messages));
            if (!"pause_turn".equals(resposta.path("stop_reason").asText())) {
                break;
            }
            messages.add(Map.of("role", "assistant", "content", resposta.path("content")));
        }
        return parsear(api.extrairTexto(resposta));
    }

    private Map<String, Object> corpo(List<Map<String, Object>> messages) {
        return Map.of(
                "model", api.modelo(),
                "max_tokens", MAX_TOKENS,
                "system", ProductPrompts.PESQUISA,
                "messages", messages,
                "tools", List.of(
                        Map.of("type", "web_search_20260209", "name", "web_search",
                                "max_uses", MAX_USOS_POR_FERRAMENTA),
                        Map.of("type", "web_fetch_20260209", "name", "web_fetch",
                                "max_uses", MAX_USOS_POR_FERRAMENTA)));
    }

    private Pesquisa parsear(String texto) {
        try {
            JsonNode n = json.readTree(RespostaJson.extrairObjeto(texto));
            List<String> imagens = new ArrayList<>();
            for (JsonNode img : n.path("imagens")) {
                String url = img.asText("");
                if (url.startsWith("http")) {
                    imagens.add(url);
                }
            }
            return new Pesquisa(
                    textoOuNull(n, "marca"),
                    textoOuNull(n, "modelo"),
                    textoOuNull(n, "categoria"),
                    textoOuNull(n, "ean"),
                    textoOuNull(n, "dadosBrutos"),
                    imagens,
                    texto);
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Não consegui parsear a pesquisa do Claude como JSON:\n" + texto, e);
        }
    }

    private String textoOuNull(JsonNode n, String campo) {
        JsonNode valor = n.path(campo);
        if (valor.isMissingNode() || valor.isNull()) {
            return null;
        }
        String texto = valor.asText().trim();
        return texto.isEmpty() || "null".equalsIgnoreCase(texto) ? null : texto;
    }
}

package com.loja.catalogbling.ia;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loja.catalogbling.domain.ConversationTurn;
import com.loja.catalogbling.domain.Product;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ConteudoIaService {

    private static final int MAX_TOKENS = 4000;

    private final ChatIa chat;
    private final ObjectMapper json = new ObjectMapper();

    public ConteudoIaService(ChatIa chat) {
        this.chat = chat;
    }

    public ConteudoGerado gerar(Product produto, String imagemBase64, String mediaType) {
        List<MensagemIa> mensagens = List.of(MensagemIa.usuario(
                ProductPrompts.primeiraMensagem(
                        produto.getDadosBrutos(), produto.getMarca(), produto.getModelo(),
                        produto.getCategoria(), produto.getEan())));

        ChatIa.ImagemAnexa imagem = imagemBase64 == null
                ? null : new ChatIa.ImagemAnexa(imagemBase64, mediaType);

        return parsear(chat.completar(ProductPrompts.SYSTEM, mensagens, imagem, MAX_TOKENS));
    }

    public ConteudoGerado revisar(Product produto, String pedidoDeAjuste) {
        List<MensagemIa> mensagens = new ArrayList<>();
        for (ConversationTurn turno : produto.getConversa()) {
            mensagens.add(new MensagemIa(turno.getPapel(), turno.getConteudo()));
        }
        mensagens.add(MensagemIa.usuario(pedidoDeAjuste));

        return parsear(chat.completar(ProductPrompts.SYSTEM, mensagens, null, MAX_TOKENS));
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
                    "Não consegui parsear a resposta da IA como JSON:\n" + texto, e);
        }
    }
}

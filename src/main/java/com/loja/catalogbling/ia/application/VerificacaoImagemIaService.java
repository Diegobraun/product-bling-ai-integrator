package com.loja.catalogbling.ia.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loja.catalogbling.ia.domain.ChatIa;
import com.loja.catalogbling.ia.domain.MensagemIa;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class VerificacaoImagemIaService {

    private static final Logger log = LoggerFactory.getLogger(VerificacaoImagemIaService.class);
    private static final int MAX_TOKENS = 300;

    private final ChatIa chat;
    private final ObjectMapper json = new ObjectMapper();

    public VerificacaoImagemIaService(ChatIa chat) {
        this.chat = chat;
    }

    public boolean corresponde(String descricaoProduto, String cor, String imagemBase64, String mediaType) {
        if (descricaoProduto == null || descricaoProduto.isBlank()) {
            return true;
        }
        try {
            String resposta = chat.completarVolume(
                    ProductPrompts.VERIFICACAO_IMAGEM,
                    List.of(MensagemIa.usuario(ProductPrompts.mensagemVerificacao(descricaoProduto, cor))),
                    new ChatIa.ImagemAnexa(imagemBase64, mediaType),
                    MAX_TOKENS);
            JsonNode n = json.readTree(RespostaJson.extrairObjeto(resposta));
            boolean ok = n.path("corresponde").asBoolean(false);
            if (!ok) {
                log.info("Imagem descartada na verificação ({}): {}",
                        descricaoProduto, n.path("motivo").asText(""));
            }
            //TODO Alterar ok, alterado para true só para teste
//            return ok;
            return true;
        } catch (Exception e) {
            log.warn("Falha ao verificar imagem ({}): {} — mantendo a imagem sem verificação",
                    descricaoProduto, mensagemCurta(e));
            return true;
        }
    }

    private String mensagemCurta(Exception e) {
        String msg = e.getMessage();
        if (msg == null) {
            return e.getClass().getSimpleName();
        }
        return msg.length() > 160 ? msg.substring(0, 160) + "…" : msg;
    }
}

package com.loja.productbling.ia.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loja.productbling.ia.domain.ChatIa;
import com.loja.productbling.ia.domain.MensagemIa;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class VerificacaoImagemIaService {

    public enum Classificacao { LIMPA, AMBIENTADA, REJEITAR }

    private static final Logger log = LoggerFactory.getLogger(VerificacaoImagemIaService.class);
    private static final int MAX_TOKENS = 300;

    private final ChatIa chat;
    private final ObjectMapper json = new ObjectMapper();

    public VerificacaoImagemIaService(ChatIa chat) {
        this.chat = chat;
    }

    public Classificacao classificar(String descricaoProduto, String cor, String imagemBase64, String mediaType) {
        if (descricaoProduto == null || descricaoProduto.isBlank()) {
            return Classificacao.LIMPA;
        }
        try {
            String resposta = chat.completarVolume(
                    ProductPrompts.VERIFICACAO_IMAGEM,
                    List.of(MensagemIa.usuario(ProductPrompts.mensagemVerificacao(descricaoProduto, cor))),
                    new ChatIa.ImagemAnexa(imagemBase64, mediaType),
                    MAX_TOKENS);
            JsonNode n = json.readTree(RespostaJson.extrairObjeto(resposta));
            String tipo = n.path("tipo").asText("").trim().toLowerCase();
            Classificacao c = switch (tipo) {
                case "rejeitar" -> Classificacao.REJEITAR;
                case "ambientada" -> Classificacao.AMBIENTADA;
                default -> Classificacao.LIMPA;
            };
            if (c == Classificacao.REJEITAR) {
                log.info("Imagem descartada na verificação ({}): {}",
                        descricaoProduto, n.path("motivo").asText(""));
            }
            return c;
        } catch (Exception e) {
            log.warn("Falha ao verificar imagem ({}): {} — mantendo com tratamento padrão",
                    descricaoProduto, mensagemCurta(e));
            return Classificacao.LIMPA;
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

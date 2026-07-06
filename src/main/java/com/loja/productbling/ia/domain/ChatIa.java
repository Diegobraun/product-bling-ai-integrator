package com.loja.productbling.ia.domain;

import java.util.List;

public interface ChatIa {

    String completar(String system, List<MensagemIa> mensagens, ImagemAnexa imagem, int maxTokens);

    default String completarVolume(String system, List<MensagemIa> mensagens, ImagemAnexa imagem, int maxTokens) {
        return completar(system, mensagens, imagem, maxTokens);
    }

    record ImagemAnexa(String base64, String mediaType) {}
}

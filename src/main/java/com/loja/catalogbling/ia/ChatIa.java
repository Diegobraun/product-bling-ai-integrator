package com.loja.catalogbling.ia;

import java.util.List;

public interface ChatIa {

    String completar(String system, List<MensagemIa> mensagens, ImagemAnexa imagem, int maxTokens);

    record ImagemAnexa(String base64, String mediaType) {}
}

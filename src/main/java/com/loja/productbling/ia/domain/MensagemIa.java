package com.loja.productbling.ia.domain;

public record MensagemIa(String papel, String conteudo) {

    public static final String USUARIO = "user";
    public static final String ASSISTENTE = "assistant";

    public static MensagemIa usuario(String conteudo) {
        return new MensagemIa(USUARIO, conteudo);
    }
}

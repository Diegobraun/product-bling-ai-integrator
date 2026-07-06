package com.loja.productbling.catalogo.domain;

public final class EntradaDeBusca {

    private final String texto;

    private EntradaDeBusca(String texto) {
        this.texto = texto;
    }

    public static EntradaDeBusca de(String texto) {
        return new EntradaDeBusca(texto);
    }

    public boolean ehLink() {
        return texto != null && texto.strip().toLowerCase().startsWith("http");
    }

    public String url() {
        if (texto == null) {
            return null;
        }
        for (String token : texto.strip().split("\\s+")) {
            if (token.toLowerCase().startsWith("http")) {
                return token;
            }
        }
        return null;
    }

    public String textoDigitado() {
        if (texto == null) {
            return "";
        }
        StringBuilder digitado = new StringBuilder();
        for (String token : texto.strip().split("\\s+")) {
            if (!token.toLowerCase().startsWith("http")) {
                digitado.append(token).append(' ');
            }
        }
        return digitado.toString().strip();
    }

    public Cor cor() {
        Cor doDigitado = Cor.deTexto(textoDigitado());
        return doDigitado.presente() ? doDigitado : Cor.deTexto(texto);
    }
}

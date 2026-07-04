package com.loja.catalogbling.ia;

final class RespostaJson {

    private RespostaJson() {
    }

    static String extrairObjeto(String texto) {
        String limpo = texto
                .replaceAll("(?s)^```json", "")
                .replaceAll("(?s)^```", "")
                .replaceAll("(?s)```$", "")
                .trim();
        int inicio = limpo.indexOf('{');
        int fim = limpo.lastIndexOf('}');
        if (inicio >= 0 && fim > inicio) {
            return limpo.substring(inicio, fim + 1);
        }
        return limpo;
    }
}

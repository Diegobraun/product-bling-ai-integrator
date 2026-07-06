package com.loja.productbling.catalogo.domain;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class Cor {

    private static final Map<String, String> CORES = Map.ofEntries(
            Map.entry("rosa", "rosa"), Map.entry("rose", "rosa"), Map.entry("pink", "rosa"),
            Map.entry("preto", "preto"), Map.entry("black", "preto"),
            Map.entry("grafite", "grafite"), Map.entry("graphite", "grafite"),
            Map.entry("branco", "branco"), Map.entry("white", "branco"),
            Map.entry("cinza", "cinza"), Map.entry("gray", "cinza"), Map.entry("grey", "cinza"),
            Map.entry("prata", "prata"), Map.entry("silver", "prata"),
            Map.entry("azul", "azul"), Map.entry("blue", "azul"),
            Map.entry("vermelho", "vermelho"), Map.entry("red", "vermelho"),
            Map.entry("verde", "verde"), Map.entry("green", "verde"),
            Map.entry("amarelo", "amarelo"), Map.entry("yellow", "amarelo"));

    private static final Cor AUSENTE = new Cor("");

    private final String valor;

    private Cor(String valor) {
        this.valor = valor;
    }

    public static Cor deTexto(String texto) {
        if (texto == null) {
            return AUSENTE;
        }
        for (String palavra : texto.toLowerCase().split("[^a-zà-ú]+")) {
            String canonica = CORES.get(palavra);
            if (canonica != null) {
                return new Cor(canonica);
            }
        }
        return AUSENTE;
    }

    public boolean presente() {
        return !valor.isBlank();
    }

    public String valor() {
        return valor;
    }

    public Set<String> tokensParaExcluir() {
        if (!presente()) {
            return Set.of();
        }
        Set<String> excluir = new HashSet<>();
        for (Map.Entry<String, String> e : CORES.entrySet()) {
            if (!e.getValue().equals(valor)) {
                excluir.add(e.getKey());
            }
        }
        return excluir;
    }
}

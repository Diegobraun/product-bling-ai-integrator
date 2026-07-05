package com.loja.catalogbling.ia.domain;

import java.util.List;

public record PesquisaDeProduto(String marca, String modelo, String categoria, String ean,
                                String dadosBrutos, List<String> imagens, List<String> paginas,
                                String respostaBruta) {
}

package com.loja.productbling.ia.domain;

import java.util.List;

public record PesquisaDeProduto(String marca, String modelo, String categoria, String ean,
                                String dadosBrutos, List<String> imagens, List<String> paginas,
                                String respostaBruta) {
}

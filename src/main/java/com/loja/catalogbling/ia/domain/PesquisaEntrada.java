package com.loja.catalogbling.ia.domain;

public record PesquisaEntrada(String nome, String tipo, String marca, String modelo,
                              String ean, String sku, String fornecedor, String fabricante) {

    public static PesquisaEntrada soNome(String nome) {
        return new PesquisaEntrada(nome, null, null, null, null, null, null, null);
    }
}

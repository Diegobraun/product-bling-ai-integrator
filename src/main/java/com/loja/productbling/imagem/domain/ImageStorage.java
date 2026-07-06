package com.loja.productbling.imagem.domain;

import java.io.IOException;

public interface ImageStorage {

    String salvar(Long productId, int indice, byte[] jpeg) throws IOException;

    byte[] ler(String nome) throws IOException;

    void remover(String nome) throws IOException;
}

package com.loja.catalogbling.imagem;

import java.io.IOException;

public interface ImageStorage {

    String salvar(Long productId, byte[] jpeg) throws IOException;

    byte[] ler(String nome) throws IOException;
}

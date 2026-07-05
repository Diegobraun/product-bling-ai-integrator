package com.loja.catalogbling.ia.domain;

public interface PesquisaWebIa {

    String pesquisar(String system, String prompt, int maxTokens);
}

package com.loja.catalogbling.ia;

import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

public final class IaHttp {

    public static final int TIMEOUT_CONEXAO_MS = 15_000;
    public static final int TIMEOUT_LEITURA_MS = 300_000;

    private IaHttp() {
    }

    public static RestClient clientePadrao() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(TIMEOUT_CONEXAO_MS);
        factory.setReadTimeout(TIMEOUT_LEITURA_MS);
        return RestClient.builder().requestFactory(factory).build();
    }
}

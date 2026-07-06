package com.loja.productbling.config;

import com.loja.productbling.ia.infrastructure.IaHttp;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class HttpClientsConfig {

    @Bean
    public RestClient restClient() {
        return IaHttp.clientePadrao();
    }
}

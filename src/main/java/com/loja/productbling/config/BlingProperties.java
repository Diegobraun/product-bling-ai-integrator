package com.loja.productbling.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "bling")
public record BlingProperties(String clientId, String clientSecret, String apiBaseUrl) {

    public BlingProperties {
        if (apiBaseUrl == null || apiBaseUrl.isBlank()) {
            apiBaseUrl = "https://api.bling.com.br/Api/v3";
        }
    }
}

package com.loja.catalogbling.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "bling")
public record BlingProperties(String clientId, String clientSecret) {
}

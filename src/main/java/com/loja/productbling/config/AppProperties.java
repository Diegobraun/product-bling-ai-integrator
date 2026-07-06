package com.loja.productbling.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(String publicBaseUrl, String storageDir) {
}

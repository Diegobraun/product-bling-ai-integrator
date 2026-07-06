package com.loja.productbling.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "gemini")
public record GeminiProperties(String apiKey, String model, List<String> fallbackModels,
                               List<String> verificacaoModels, String baseUrl) {

    public GeminiProperties {
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "https://generativelanguage.googleapis.com/v1beta/models/";
        }
    }
}

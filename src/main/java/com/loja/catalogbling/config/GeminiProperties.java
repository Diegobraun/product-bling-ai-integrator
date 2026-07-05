package com.loja.catalogbling.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "gemini")
public record GeminiProperties(String apiKey, String model, List<String> fallbackModels,
                               List<String> verificacaoModels) {
}

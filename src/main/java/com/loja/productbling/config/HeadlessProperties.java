package com.loja.productbling.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "imagem.headless")
public record HeadlessProperties(boolean enabled, boolean stealth, boolean headed,
                                 String channel, int timeoutMs, int esperaRenderMs) {

    public HeadlessProperties {
        if (channel == null || channel.isBlank()) {
            channel = "chromium";
        }
        if (timeoutMs <= 0) {
            timeoutMs = 30_000;
        }
        if (esperaRenderMs <= 0) {
            esperaRenderMs = 3_500;
        }
    }
}

package com.loja.productbling;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class ProductBlingAiApplication {
    public static void main(String[] args) {
        SpringApplication.run(ProductBlingAiApplication.class, args);
    }
}

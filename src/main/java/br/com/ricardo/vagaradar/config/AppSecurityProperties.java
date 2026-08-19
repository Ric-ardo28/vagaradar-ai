package br.com.ricardo.vagaradar.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security")
public record AppSecurityProperties(
        String adminUsername,
        String adminPassword
) {
}

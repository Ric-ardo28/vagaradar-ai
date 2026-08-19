package br.com.ricardo.vagaradar.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "gmail.push")
public record GmailPushProperties(
        boolean enabled,
        String topicName,
        String audience,
        String serviceAccountEmail,
        Duration renewalFixedDelay,
        Duration renewalInitialDelay
) {
}

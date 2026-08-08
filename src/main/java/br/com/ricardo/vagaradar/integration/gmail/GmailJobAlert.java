package br.com.ricardo.vagaradar.integration.gmail;

import java.time.Instant;

public record GmailJobAlert(
        String messageId,
        String threadId,
        String subject,
        String plainText,
        String html,
        Instant receivedAt
) {
}

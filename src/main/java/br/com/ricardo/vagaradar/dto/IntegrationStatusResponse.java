package br.com.ricardo.vagaradar.dto;

import java.time.Instant;

public record IntegrationStatusResponse(
        boolean gmailConnected,
        String gmailAccount,
        boolean discordConfigured,
        int discordMinimumScore,
        Instant ultimaAnaliseEm
) {
}

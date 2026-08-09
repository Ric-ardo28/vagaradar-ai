package br.com.ricardo.vagaradar.dto;

public record IntegrationStatusResponse(
        boolean gmailConnected,
        String gmailAccount,
        boolean discordConfigured,
        int discordMinimumScore
) {
}

package br.com.ricardo.vagaradar.integration.gmail;

public record GmailImportResult(
        int mensagensLidas,
        int vagasImportadas,
        int vagasIgnoradas
) {
}

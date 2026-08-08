package br.com.ricardo.vagaradar.service;

import br.com.ricardo.vagaradar.integration.gmail.GmailImportResult;

public record GmailProcessingResult(
        GmailImportResult importacao,
        int vagasAnalisadas
) {
}

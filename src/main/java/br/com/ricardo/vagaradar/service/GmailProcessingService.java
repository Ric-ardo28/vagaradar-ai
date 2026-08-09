package br.com.ricardo.vagaradar.service;

import br.com.ricardo.vagaradar.integration.gmail.GmailImportResult;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(prefix = "gmail", name = "oauth-enabled", havingValue = "true")
public class GmailProcessingService {

    private final GmailImportService gmailImportService;
    private final VagaService vagaService;

    public GmailProcessingService(GmailImportService gmailImportService, VagaService vagaService) {
        this.gmailImportService = gmailImportService;
        this.vagaService = vagaService;
    }

    public GmailProcessingResult processarAlertas(String accessToken) {
        GmailImportResult importacao = gmailImportService.importarAlertas(accessToken);
        for (Long vagaId : importacao.vagasImportadasIds()) {
            vagaService.analisar(vagaId);
        }
        return new GmailProcessingResult(importacao, importacao.vagasImportadasIds().size());
    }
}

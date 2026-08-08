package br.com.ricardo.vagaradar.service;

import br.com.ricardo.vagaradar.integration.gmail.GmailImportResult;
import org.springframework.stereotype.Service;

@Service
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

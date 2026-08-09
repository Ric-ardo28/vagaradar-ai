package br.com.ricardo.vagaradar.service;

import br.com.ricardo.vagaradar.integration.gmail.GmailImportResult;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;

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
        List<Long> vagasPendentes = vagaService.listarIdsPendentesDeAnalise();
        for (Long vagaId : vagasPendentes) {
            vagaService.analisar(vagaId);
        }
        return new GmailProcessingResult(importacao, vagasPendentes.size());
    }
}

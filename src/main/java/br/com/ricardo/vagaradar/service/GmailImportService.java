package br.com.ricardo.vagaradar.service;

import br.com.ricardo.vagaradar.dto.VagaCreateRequest;
import br.com.ricardo.vagaradar.integration.gmail.GmailImportResult;
import br.com.ricardo.vagaradar.integration.gmail.GmailJobAlert;
import br.com.ricardo.vagaradar.integration.gmail.GmailJobAlertParser;
import br.com.ricardo.vagaradar.integration.gmail.GmailReader;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.ArrayList;
import java.time.Duration;
import java.time.Instant;

@Service
@ConditionalOnProperty(prefix = "gmail", name = "oauth-enabled", havingValue = "true")
public class GmailImportService {

    private static final Duration INITIAL_LOOKBACK = Duration.ofDays(1);

    private final GmailReader gmailReader;
    private final VagaService vagaService;
    private final GmailSyncStateService gmailSyncStateService;
    private final GmailJobAlertParser parser = new GmailJobAlertParser();

    public GmailImportService(
            GmailReader gmailReader,
            VagaService vagaService,
            GmailSyncStateService gmailSyncStateService
    ) {
        this.gmailReader = gmailReader;
        this.vagaService = vagaService;
        this.gmailSyncStateService = gmailSyncStateService;
    }

    public GmailImportResult importarAlertas(String accessToken) {
        Instant inicioDaSincronizacao = Instant.now();
        Instant recebidosDepoisDe = gmailSyncStateService.ultimoSucesso()
                .orElse(inicioDaSincronizacao.minus(INITIAL_LOOKBACK));
        List<GmailJobAlert> alertas = gmailReader.buscarAlertasDetalhados(accessToken, recebidosDepoisDe);
        int vagasImportadas = 0;
        int vagasIgnoradas = 0;
        List<Long> vagasImportadasIds = new ArrayList<>();

        for (GmailJobAlert alerta : alertas) {
            List<VagaCreateRequest> vagas = parser.extrairVagas(alerta);
            if (vagas.isEmpty()) {
                vagasIgnoradas++;
                continue;
            }
            for (VagaCreateRequest vaga : vagas) {
                var vagaImportada = vagaService.criarSeNova(vaga);
                if (vagaImportada.isPresent()) {
                    vagasImportadas++;
                    vagasImportadasIds.add(vagaImportada.get().id());
                } else {
                    vagasIgnoradas++;
                }
            }
        }

        gmailSyncStateService.registrarSucesso(inicioDaSincronizacao);
        return new GmailImportResult(alertas.size(), vagasImportadas, vagasIgnoradas, vagasImportadasIds);
    }
}

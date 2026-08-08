package br.com.ricardo.vagaradar.service;

import br.com.ricardo.vagaradar.dto.VagaCreateRequest;
import br.com.ricardo.vagaradar.integration.gmail.GmailImportResult;
import br.com.ricardo.vagaradar.integration.gmail.GmailJobAlert;
import br.com.ricardo.vagaradar.integration.gmail.GmailJobAlertParser;
import br.com.ricardo.vagaradar.integration.gmail.GmailReader;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.ArrayList;

@Service
public class GmailImportService {

    private final GmailReader gmailReader;
    private final VagaService vagaService;
    private final GmailJobAlertParser parser = new GmailJobAlertParser();

    public GmailImportService(GmailReader gmailReader, VagaService vagaService) {
        this.gmailReader = gmailReader;
        this.vagaService = vagaService;
    }

    public GmailImportResult importarAlertas(String accessToken) {
        List<GmailJobAlert> alertas = gmailReader.buscarAlertasDetalhados(accessToken);
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

        return new GmailImportResult(alertas.size(), vagasImportadas, vagasIgnoradas, vagasImportadasIds);
    }
}

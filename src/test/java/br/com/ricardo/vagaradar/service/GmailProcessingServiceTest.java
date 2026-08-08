package br.com.ricardo.vagaradar.service;

import br.com.ricardo.vagaradar.integration.gmail.GmailImportResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class GmailProcessingServiceTest {

    @Mock
    private GmailImportService gmailImportService;

    @Mock
    private VagaService vagaService;

    @Test
    void deveAnalisarSomenteAsVagasImportadasNaExecucaoAtual() {
        GmailImportResult importacao = new GmailImportResult(3, 2, 1, List.of(11L, 12L));
        given(gmailImportService.importarAlertas("token")).willReturn(importacao);
        GmailProcessingService service = new GmailProcessingService(gmailImportService, vagaService);

        GmailProcessingResult result = service.processarAlertas("token");

        assertThat(result).isEqualTo(new GmailProcessingResult(importacao, 2));
        verify(vagaService).analisar(11L);
        verify(vagaService).analisar(12L);
    }
}

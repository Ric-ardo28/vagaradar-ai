package br.com.ricardo.vagaradar.service;

import br.com.ricardo.vagaradar.integration.gmail.GmailImportResult;
import br.com.ricardo.vagaradar.integration.gmail.GmailJobAlert;
import br.com.ricardo.vagaradar.integration.gmail.GmailReader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class GmailImportServiceTest {

    @Mock
    private GmailReader gmailReader;

    @Mock
    private VagaService vagaService;

    @Test
    void deveImportarSomenteVagasAindaNaoCadastradas() {
        GmailJobAlert alert = new GmailJobAlert(
                "message-1",
                "thread-1",
                "Vagas Java",
                "Confira as oportunidades.",
                """
                        <a href="https://www.linkedin.com/jobs/view/123">Desenvolvedor Java</a>
                        <a href="https://www.linkedin.com/jobs/view/456">Backend Java</a>
                        """,
                Instant.parse("2026-08-08T12:00:00Z")
        );
        given(gmailReader.buscarAlertasDetalhados("token")).willReturn(List.of(alert));
        given(vagaService.criarSeNova(any())).willReturn(
                Optional.of(new br.com.ricardo.vagaradar.dto.VagaResponse(
                        10L, null, "Desenvolvedor Java", "Empresa", "Descrição", null,
                        br.com.ricardo.vagaradar.entity.ModeloTrabalho.NAO_INFORMADO,
                        "https://www.linkedin.com/jobs/view/123", Instant.now(), Instant.now(),
                        br.com.ricardo.vagaradar.entity.StatusVaga.RECEBIDA, null
                ))
        ).willReturn(Optional.empty());
        GmailImportService service = new GmailImportService(gmailReader, vagaService);

        GmailImportResult result = service.importarAlertas("token");

        assertThat(result).isEqualTo(new GmailImportResult(1, 1, 1, List.of(10L)));
        ArgumentCaptor<br.com.ricardo.vagaradar.dto.VagaCreateRequest> captor = ArgumentCaptor.forClass(
                br.com.ricardo.vagaradar.dto.VagaCreateRequest.class
        );
        verify(vagaService, times(2)).criarSeNova(captor.capture());
        assertThat(captor.getAllValues())
                .extracting(br.com.ricardo.vagaradar.dto.VagaCreateRequest::link)
                .containsExactly(
                        "https://www.linkedin.com/jobs/view/123",
                        "https://www.linkedin.com/jobs/view/456"
                );
    }
}

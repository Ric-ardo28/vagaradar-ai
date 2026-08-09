package br.com.ricardo.vagaradar.integration.gmail;

import br.com.ricardo.vagaradar.dto.VagaCreateRequest;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GmailJobAlertParserTest {

    private final GmailJobAlertParser parser = new GmailJobAlertParser();

    @Test
    void deveExtrairUmaVagaParaCadaLinkDoLinkedInNoAlerta() {
        GmailJobAlert alert = new GmailJobAlert(
                "message-1",
                "thread-1",
                "Novas vagas de Java",
                "Confira as vagas indicadas para você.",
                """
                        <a href="https://www.linkedin.com/jobs/view/123?trackingId=abc">Desenvolvedor Java Júnior</a>
                        <a href="https://www.linkedin.com/comm/jobs/view/456">Backend Engineer</a>
                        """,
                Instant.parse("2026-08-08T12:00:00Z")
        );

        List<VagaCreateRequest> vagas = parser.extrairVagas(alert);

        assertThat(vagas).hasSize(2);
        assertThat(vagas)
                .extracting(VagaCreateRequest::cargo)
                .containsExactly("Desenvolvedor Java Júnior", "Backend Engineer");
        assertThat(vagas)
                .extracting(VagaCreateRequest::link)
                .containsExactly(
                        "https://www.linkedin.com/jobs/view/123",
                        "https://www.linkedin.com/jobs/view/456"
                );
    }

    @Test
    void deveTratarLinksComParametrosDiferentesComoUmaUnicaVaga() {
        GmailJobAlert alert = new GmailJobAlert(
                "message-1", "thread-1", "Novas vagas de Java", "",
                """
                        <a href="https://www.linkedin.com/jobs/view/123?trackingId=primeiro">Desenvolvedor Java</a>
                        <a href="https://www.linkedin.com/jobs/view/123?trackingId=segundo">Desenvolvedor Java</a>
                        """,
                Instant.parse("2026-08-08T12:00:00Z")
        );

        assertThat(parser.extrairVagas(alert)).singleElement()
                .extracting(VagaCreateRequest::link)
                .isEqualTo("https://www.linkedin.com/jobs/view/123");
    }

    @Test
    void deveIgnorarAlertaSemLinkDeVagaDoLinkedIn() {
        GmailJobAlert alert = new GmailJobAlert(
                "message-1", "thread-1", "Boletim", "Conteúdo sem vagas", "", Instant.now()
        );

        assertThat(parser.extrairVagas(alert)).isEmpty();
    }

    @Test
    void deveIgnorarEmailDeConfirmacaoDeAlerta() {
        GmailJobAlert alert = new GmailJobAlert(
                "message-1", "thread-1",
                "Ricardo: foi criado seu alerta de vaga para Java Jr em: Brasil",
                "Seu alerta foi criado com sucesso.", "", Instant.now()
        );

        assertThat(parser.extrairVagas(alert)).isEmpty();
    }
}

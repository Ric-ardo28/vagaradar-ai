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
    void devePriorizarOTituloIndividualDoTextoQuandoOLinkHtmlTemTextoGenerico() {
        GmailJobAlert alert = new GmailJobAlert(
                "message-1", "thread-1", "Alerta de vagas", 
                """
                        Novas vagas correspondem às suas preferências.

                        Desenvolvedor Java Júnior
                        Code Group
                        São Paulo e Região
                        Visualizar vaga: https://www.linkedin.com/jobs/view/123?trackingId=abc
                        --------------------
                        Desenvolvedor Java
                        DQR Tech
                        São Paulo, Brasil
                        Visualizar vaga: https://www.linkedin.com/jobs/view/456?trackingId=def
                        """,
                """
                        <a href="https://www.linkedin.com/jobs/view/123">Desenvolvedor Java Júnior na empresa Code Group</a>
                        <a href="https://www.linkedin.com/jobs/view/456">Desenvolvedor Java Júnior na empresa Code Group</a>
                        """,
                Instant.parse("2026-08-08T12:00:00Z")
        );

        assertThat(parser.extrairVagas(alert))
                .extracting(VagaCreateRequest::cargo)
                .containsExactly("Desenvolvedor Java Júnior", "Desenvolvedor Java");
    }

    @Test
    void deveUsarADataDePublicacaoInformadaNoTrechoDaVaga() {
        GmailJobAlert alert = new GmailJobAlert(
                "message-1", "thread-1", "Alerta de vagas",
                """
                        Desenvolvedor Java Júnior
                        Publicada há 2 dias
                        Visualizar vaga: https://www.linkedin.com/jobs/view/123
                        """,
                "", Instant.parse("2026-08-08T12:00:00Z")
        );

        assertThat(parser.extrairVagas(alert)).singleElement()
                .extracting(VagaCreateRequest::dataPublicacao)
                .isEqualTo(Instant.parse("2026-08-06T12:00:00Z"));
    }

    @Test
    void naoDevePreencherDataDePublicacaoQuandoOEmailNaoInformaAData() {
        GmailJobAlert alert = new GmailJobAlert(
                "message-1", "thread-1", "Alerta de vagas",
                "Desenvolvedor Java Júnior\\nVisualizar vaga: https://www.linkedin.com/jobs/view/123",
                "", Instant.parse("2026-08-08T12:00:00Z")
        );

        assertThat(parser.extrairVagas(alert)).singleElement()
                .extracting(VagaCreateRequest::dataPublicacao)
                .isNull();
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

    @Test
    void deveIgnorarResumoDeVagasMesmoQuandoPossuiLinkDireto() {
        GmailJobAlert alert = new GmailJobAlert(
                "message-1", "thread-1", "Alerta de vagas",
                "", """
                        <a href="https://www.linkedin.com/jobs/view/123">5 vagas novas correspondem às suas preferências.</a>
                        """, Instant.now()
        );

        assertThat(parser.extrairVagas(alert)).isEmpty();
    }

    @Test
    void deveIgnorarLinkParaBuscaGeralDoLinkedIn() {
        GmailJobAlert alert = new GmailJobAlert(
                "message-1", "thread-1", "Alerta de vagas",
                "Ver todas as vagas no LinkedIn: https://www.linkedin.com/comm/jobs/search-results/?keywords=Java",
                "", Instant.now()
        );

        assertThat(parser.extrairVagas(alert)).isEmpty();
    }

    @Test
    void deveIgnorarLinkDeVagaQuandoORotuloEUmAtalhoParaTodasAsVagas() {
        GmailJobAlert alert = new GmailJobAlert(
                "message-1", "thread-1", "Alerta de vagas",
                "", """
                        <a href="https://www.linkedin.com/jobs/view/123?trackingId=abc">
                          Ver todas as vagas https://www.linkedin.com/jobs/search-results/?keywords=estagio+backend
                        </a>
                        """, Instant.now()
        );

        assertThat(parser.extrairVagas(alert)).isEmpty();
    }
}

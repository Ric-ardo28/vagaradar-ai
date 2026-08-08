package br.com.ricardo.vagaradar.scheduler;

import br.com.ricardo.vagaradar.service.GmailProcessingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@ConditionalOnProperty(prefix = "gmail.scheduler", name = "enabled", havingValue = "true")
public class GmailProcessingScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GmailProcessingScheduler.class);

    private final JdbcOperations jdbcOperations;
    private final OAuth2AuthorizedClientManager authorizedClientManager;
    private final GmailProcessingService gmailProcessingService;

    public GmailProcessingScheduler(
            JdbcOperations jdbcOperations,
            OAuth2AuthorizedClientManager authorizedClientManager,
            GmailProcessingService gmailProcessingService
    ) {
        this.jdbcOperations = jdbcOperations;
        this.authorizedClientManager = authorizedClientManager;
        this.gmailProcessingService = gmailProcessingService;
    }

    @Scheduled(
            fixedDelayString = "${gmail.scheduler.fixed-delay:PT6H}",
            initialDelayString = "${gmail.scheduler.initial-delay:PT5M}"
    )
    public void processarNovosAlertas() {
        buscarPrincipalAutorizado().ifPresentOrElse(this::processar, () ->
                LOGGER.warn("Agendamento do Gmail ignorado: nenhuma conta Google foi autorizada ainda.")
        );
    }

    private Optional<String> buscarPrincipalAutorizado() {
        return jdbcOperations.query(
                """
                        SELECT principal_name
                        FROM oauth2_authorized_client
                        WHERE client_registration_id = 'google'
                        ORDER BY created_at DESC
                        LIMIT 1
                        """,
                resultSet -> resultSet.next()
                        ? Optional.of(resultSet.getString("principal_name"))
                        : Optional.empty()
        );
    }

    private void processar(String principalName) {
        try {
            OAuth2AuthorizedClient client = authorizedClientManager.authorize(
                    OAuth2AuthorizeRequest.withClientRegistrationId("google")
                            .principal(principalName)
                            .build()
            );
            if (client == null) {
                LOGGER.error("Agendamento do Gmail não obteve um cliente autorizado para {}.", principalName);
                return;
            }

            var result = gmailProcessingService.processarAlertas(client.getAccessToken().getTokenValue());
            LOGGER.info(
                    "Agendamento do Gmail concluído: {} mensagens, {} vagas importadas e {} analisadas.",
                    result.importacao().mensagensLidas(),
                    result.importacao().vagasImportadas(),
                    result.vagasAnalisadas()
            );
        } catch (RuntimeException exception) {
            LOGGER.error("Agendamento do Gmail falhou.", exception);
        }
    }
}

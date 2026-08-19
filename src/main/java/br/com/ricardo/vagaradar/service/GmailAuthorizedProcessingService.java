package br.com.ricardo.vagaradar.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@ConditionalOnProperty(prefix = "gmail", name = "oauth-enabled", havingValue = "true")
public class GmailAuthorizedProcessingService {

    private static final Logger LOGGER = LoggerFactory.getLogger(GmailAuthorizedProcessingService.class);

    private final JdbcOperations jdbcOperations;
    private final OAuth2AuthorizedClientManager authorizedClientManager;
    private final GmailProcessingService gmailProcessingService;

    public GmailAuthorizedProcessingService(
            JdbcOperations jdbcOperations,
            OAuth2AuthorizedClientManager authorizedClientManager,
            GmailProcessingService gmailProcessingService
    ) {
        this.jdbcOperations = jdbcOperations;
        this.authorizedClientManager = authorizedClientManager;
        this.gmailProcessingService = gmailProcessingService;
    }

    public Optional<GmailProcessingResult> processarContaAutorizada() {
        return buscarPrincipalAutorizado().flatMap(this::processar);
    }

    public Optional<String> buscarPrincipalAutorizado() {
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

    public Optional<OAuth2AuthorizedClient> buscarClienteAutorizado() {
        return buscarPrincipalAutorizado().flatMap(principalName -> {
            OAuth2AuthorizedClient client = authorizedClientManager.authorize(
                    OAuth2AuthorizeRequest.withClientRegistrationId("google").principal(principalName).build()
            );
            return Optional.ofNullable(client);
        });
    }

    private Optional<GmailProcessingResult> processar(String principalName) {
        try {
            OAuth2AuthorizedClient client = authorizedClientManager.authorize(
                    OAuth2AuthorizeRequest.withClientRegistrationId("google").principal(principalName).build()
            );
            if (client == null) {
                LOGGER.error("Não foi possível renovar a autorização do Gmail.");
                return Optional.empty();
            }
            return Optional.of(gmailProcessingService.processarAlertas(client.getAccessToken().getTokenValue()));
        } catch (RuntimeException exception) {
            LOGGER.error("O processamento acionado pelo Gmail falhou.", exception);
            return Optional.empty();
        }
    }
}

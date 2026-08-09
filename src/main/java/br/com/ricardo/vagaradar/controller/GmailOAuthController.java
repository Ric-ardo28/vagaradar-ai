package br.com.ricardo.vagaradar.controller;

import br.com.ricardo.vagaradar.config.DiscordProperties;
import br.com.ricardo.vagaradar.dto.IntegrationStatusResponse;
import br.com.ricardo.vagaradar.integration.gmail.GmailMessageSummary;
import br.com.ricardo.vagaradar.integration.gmail.GmailReader;
import br.com.ricardo.vagaradar.integration.gmail.GmailImportResult;
import br.com.ricardo.vagaradar.service.GmailImportService;
import br.com.ricardo.vagaradar.service.GmailProcessingResult;
import br.com.ricardo.vagaradar.service.GmailProcessingService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.security.oauth2.core.user.OAuth2User;
import jakarta.persistence.EntityManager;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@ConditionalOnProperty(prefix = "gmail", name = "oauth-enabled", havingValue = "true")
public class GmailOAuthController {

    private final GmailReader gmailReader;
    private final GmailImportService gmailImportService;
    private final GmailProcessingService gmailProcessingService;
    private final EntityManager entityManager;
    private final DiscordProperties discordProperties;
    private final OAuth2AuthorizedClientService authorizedClientService;

    public GmailOAuthController(
            GmailReader gmailReader,
            GmailImportService gmailImportService,
            GmailProcessingService gmailProcessingService,
            EntityManager entityManager,
            DiscordProperties discordProperties,
            OAuth2AuthorizedClientService authorizedClientService
    ) {
        this.gmailReader = gmailReader;
        this.gmailImportService = gmailImportService;
        this.gmailProcessingService = gmailProcessingService;
        this.entityManager = entityManager;
        this.discordProperties = discordProperties;
        this.authorizedClientService = authorizedClientService;
    }

    @GetMapping("/api/gmail/connect")
    Map<String, String> connect() {
        return Map.of("authorizationUrl", "/oauth2/authorization/google");
    }

    @GetMapping("/api/gmail/connected")
    Map<String, String> connected(@AuthenticationPrincipal OAuth2User user) {
        return Map.of("message", "Gmail conectado para " + user.getAttribute("email") + ".");
    }

    @GetMapping("/api/integracoes/status")
    @Transactional(readOnly = true)
    IntegrationStatusResponse statusDasIntegracoes() {
        String principalName = (String) entityManager.createNativeQuery(
                """
                SELECT principal_name
                FROM oauth2_authorized_client
                WHERE client_registration_id = 'google'
                ORDER BY created_at DESC
                LIMIT 1
                """
        ).getResultStream().findFirst().orElse(null);
        String gmailAccount = buscarEmailDaConta(principalName);
        boolean discordConfigured = discordProperties.webhookUrl() != null
                && !discordProperties.webhookUrl().isBlank();
        return new IntegrationStatusResponse(
                gmailAccount != null,
                gmailAccount,
                discordConfigured,
                discordProperties.minimumScore()
        );
    }

    private String buscarEmailDaConta(String principalName) {
        if (principalName == null || principalName.isBlank()) {
            return null;
        }

        OAuth2AuthorizedClient client = authorizedClientService.loadAuthorizedClient("google", principalName);
        if (client != null) {
            try {
                String email = gmailReader.buscarEmailDaConta(client.getAccessToken().getTokenValue());
                if (!email.isBlank()) {
                    return email;
                }
            } catch (RuntimeException ignored) {
                // Mantém a conexão visível mesmo se o token precisar ser renovado em seguida.
            }
        }

        return principalName.contains("@") ? principalName : "Conta Gmail conectada";
    }

    @GetMapping("/api/gmail/alerts")
    List<GmailMessageSummary> buscarAlertas(
            @RegisteredOAuth2AuthorizedClient("google") OAuth2AuthorizedClient client
    ) {
        return gmailReader.buscarAlertas(client.getAccessToken().getTokenValue());
    }

    @PostMapping("/api/gmail/import")
    GmailImportResult importarAlertas(
            @RegisteredOAuth2AuthorizedClient("google") OAuth2AuthorizedClient client
    ) {
        return gmailImportService.importarAlertas(client.getAccessToken().getTokenValue());
    }

    @PostMapping("/api/gmail/process")
    GmailProcessingResult processarAlertas(
            @RegisteredOAuth2AuthorizedClient("google") OAuth2AuthorizedClient client
    ) {
        return gmailProcessingService.processarAlertas(client.getAccessToken().getTokenValue());
    }
}

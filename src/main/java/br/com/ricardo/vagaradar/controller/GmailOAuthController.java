package br.com.ricardo.vagaradar.controller;

import br.com.ricardo.vagaradar.integration.gmail.GmailMessageSummary;
import br.com.ricardo.vagaradar.integration.gmail.GmailReader;
import br.com.ricardo.vagaradar.integration.gmail.GmailImportResult;
import br.com.ricardo.vagaradar.service.GmailImportService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.security.oauth2.core.user.OAuth2User;
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

    public GmailOAuthController(GmailReader gmailReader, GmailImportService gmailImportService) {
        this.gmailReader = gmailReader;
        this.gmailImportService = gmailImportService;
    }

    @GetMapping("/api/gmail/connect")
    Map<String, String> connect() {
        return Map.of("authorizationUrl", "/oauth2/authorization/google");
    }

    @GetMapping("/api/gmail/connected")
    Map<String, String> connected(@AuthenticationPrincipal OAuth2User user) {
        return Map.of("message", "Gmail conectado para " + user.getAttribute("email") + ".");
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
}

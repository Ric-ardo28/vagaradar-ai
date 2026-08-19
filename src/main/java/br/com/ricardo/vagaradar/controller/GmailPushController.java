package br.com.ricardo.vagaradar.controller;

import br.com.ricardo.vagaradar.service.GmailAuthorizedProcessingService;
import br.com.ricardo.vagaradar.service.PubSubPushAuthenticationService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@RestController
@ConditionalOnProperty(prefix = "gmail.push", name = "enabled", havingValue = "true")
public class GmailPushController {

    private final PubSubPushAuthenticationService authenticationService;
    private final GmailAuthorizedProcessingService authorizedProcessingService;
    private final ObjectMapper objectMapper;

    public GmailPushController(
            PubSubPushAuthenticationService authenticationService,
            GmailAuthorizedProcessingService authorizedProcessingService,
            ObjectMapper objectMapper
    ) {
        this.authenticationService = authenticationService;
        this.authorizedProcessingService = authorizedProcessingService;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/api/gmail/push")
    ResponseEntity<Void> receberNotificacao(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @RequestBody JsonNode envelope
    ) {
        authenticationService.validar(authorization);
        validarEnvelope(envelope);
        authorizedProcessingService.processarContaAutorizada();
        return ResponseEntity.noContent().build();
    }

    private void validarEnvelope(JsonNode envelope) {
        String data = envelope.path("message").path("data").asText();
        if (data.isBlank()) {
            throw new IllegalArgumentException("A notificação do Pub/Sub não contém dados do Gmail.");
        }
        try {
            JsonNode notification = objectMapper.readTree(new String(Base64.getUrlDecoder().decode(data), StandardCharsets.UTF_8));
            if (notification.path("emailAddress").asText().isBlank() || notification.path("historyId").asText().isBlank()) {
                throw new IllegalArgumentException("A notificação do Gmail está incompleta.");
            }
        } catch (IllegalArgumentException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalArgumentException("A notificação do Gmail é inválida.", exception);
        }
    }
}

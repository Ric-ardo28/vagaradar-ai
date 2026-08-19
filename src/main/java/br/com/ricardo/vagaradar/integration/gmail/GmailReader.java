package br.com.ricardo.vagaradar.integration.gmail;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Component
public class GmailReader {

    private final RestClient restClient;

    public GmailReader(RestClient.Builder builder) {
        this.restClient = builder.baseUrl("https://gmail.googleapis.com/gmail/v1").build();
    }

    public List<GmailMessageSummary> buscarAlertas(String accessToken) {
        JsonNode response = buscarListaDeMensagens(accessToken, Instant.now().minus(Duration.ofDays(1)));

        List<GmailMessageSummary> messages = new ArrayList<>();
        if (response != null) {
            for (JsonNode message : response.path("messages")) {
                messages.add(new GmailMessageSummary(
                        message.path("id").asText(),
                        message.path("threadId").asText(),
                        "Mensagem encontrada; importação detalhada será adicionada no próximo checkpoint."
                ));
            }
        }
        return messages;
    }

    public String buscarEmailDaConta(String accessToken) {
        try {
            JsonNode profile = restClient.get()
                    .uri("/users/me/profile")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .body(JsonNode.class);
            return profile == null ? "" : profile.path("emailAddress").asText("");
        } catch (RestClientException exception) {
            throw new GmailIntegrationException("Não foi possível identificar a conta do Gmail conectada.", exception);
        }
    }

    public GmailWatch iniciarMonitoramentoDaCaixaDeEntrada(String accessToken, String topicName) {
        try {
            JsonNode response = restClient.post()
                    .uri("/users/me/watch")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .body(java.util.Map.of(
                            "topicName", topicName,
                            "labelIds", List.of("INBOX"),
                            "labelFilterBehavior", "INCLUDE"
                    ))
                    .retrieve()
                    .body(JsonNode.class);
            if (response == null || response.path("historyId").asText().isBlank()) {
                throw new GmailIntegrationException("O Gmail não retornou o estado do monitoramento.", null);
            }
            return new GmailWatch(response.path("historyId").asText(), response.path("expiration").asLong());
        } catch (RestClientException exception) {
            throw new GmailIntegrationException("Não foi possível ativar o monitoramento de e-mails no Gmail.", exception);
        }
    }

    public List<GmailJobAlert> buscarAlertasDetalhados(String accessToken, Instant recebidosDepoisDe) {
        JsonNode response = buscarListaDeMensagens(accessToken, recebidosDepoisDe);
        List<GmailJobAlert> messages = new ArrayList<>();

        for (JsonNode message : response.path("messages")) {
            String messageId = message.path("id").asText();
            if (!messageId.isBlank()) {
                messages.add(buscarMensagem(accessToken, messageId));
            }
        }
        return messages;
    }

    private JsonNode buscarListaDeMensagens(String accessToken, Instant recebidosDepoisDe) {
        try {
            JsonNode response = restClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/users/me/messages")
                            .queryParam("q", consultaDeAlertasRecentes(recebidosDepoisDe))
                            .queryParam("maxResults", 100)
                            .build())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .body(JsonNode.class);
            return response == null ? com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.objectNode() : response;
        } catch (RestClientException exception) {
            throw new GmailIntegrationException("Não foi possível consultar os alertas no Gmail.", exception);
        }
    }

    private String consultaDeAlertasRecentes(Instant recebidosDepoisDe) {
        return "from:jobalerts-noreply@linkedin.com after:" + recebidosDepoisDe.getEpochSecond();
    }

    private GmailJobAlert buscarMensagem(String accessToken, String messageId) {
        try {
            JsonNode message = restClient.get()
                    .uri("/users/me/messages/{messageId}?format=full", messageId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .body(JsonNode.class);

            if (message == null) {
                throw new GmailIntegrationException("O Gmail retornou uma mensagem vazia.", null);
            }

            JsonNode payload = message.path("payload");
            return new GmailJobAlert(
                    message.path("id").asText(),
                    message.path("threadId").asText(),
                    valorDoCabecalho(payload, "Subject"),
                    extrairCorpo(payload, "text/plain"),
                    extrairCorpo(payload, "text/html"),
                    dataInterna(message)
            );
        } catch (RestClientException exception) {
            throw new GmailIntegrationException("Não foi possível ler o conteúdo de um alerta do Gmail.", exception);
        }
    }

    private String valorDoCabecalho(JsonNode payload, String nome) {
        for (JsonNode header : payload.path("headers")) {
            if (nome.equalsIgnoreCase(header.path("name").asText())) {
                return header.path("value").asText();
            }
        }
        return "";
    }

    private String extrairCorpo(JsonNode part, String mimeType) {
        StringBuilder result = new StringBuilder();
        extrairCorpo(part, mimeType, result);
        return result.toString();
    }

    private void extrairCorpo(JsonNode part, String mimeType, StringBuilder result) {
        if (mimeType.equalsIgnoreCase(part.path("mimeType").asText())) {
            String data = part.path("body").path("data").asText();
            if (!data.isBlank()) {
                result.append(decodificarBase64Url(data)).append('\n');
            }
        }
        for (JsonNode child : part.path("parts")) {
            extrairCorpo(child, mimeType, result);
        }
    }

    private String decodificarBase64Url(String value) {
        return new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8);
    }

    private Instant dataInterna(JsonNode message) {
        long epochMillis = message.path("internalDate").asLong(0);
        return epochMillis > 0 ? Instant.ofEpochMilli(epochMillis) : Instant.now();
    }
}

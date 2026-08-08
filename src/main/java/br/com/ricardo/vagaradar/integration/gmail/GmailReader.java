package br.com.ricardo.vagaradar.integration.gmail;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
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
        JsonNode response = buscarListaDeMensagens(accessToken);

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

    public List<GmailJobAlert> buscarAlertasDetalhados(String accessToken) {
        JsonNode response = buscarListaDeMensagens(accessToken);
        List<GmailJobAlert> messages = new ArrayList<>();

        for (JsonNode message : response.path("messages")) {
            String messageId = message.path("id").asText();
            if (!messageId.isBlank()) {
                messages.add(buscarMensagem(accessToken, messageId));
            }
        }
        return messages;
    }

    private JsonNode buscarListaDeMensagens(String accessToken) {
        try {
            JsonNode response = restClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/users/me/messages")
                            .queryParam("q", "(subject:Java OR subject:Backend OR subject:Spring) newer_than:30d")
                            .queryParam("maxResults", 20)
                            .build())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .body(JsonNode.class);
            return response == null ? com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.objectNode() : response;
        } catch (RestClientException exception) {
            throw new GmailIntegrationException("Não foi possível consultar os alertas no Gmail.", exception);
        }
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

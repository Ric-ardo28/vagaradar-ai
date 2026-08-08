package br.com.ricardo.vagaradar.integration.gmail;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;

@Component
public class GmailReader {

    private final RestClient restClient;

    public GmailReader(RestClient.Builder builder) {
        this.restClient = builder.baseUrl("https://gmail.googleapis.com/gmail/v1").build();
    }

    public List<GmailMessageSummary> buscarAlertas(String accessToken) {
        JsonNode response = restClient.get()
                .uri(uriBuilder -> uriBuilder.path("/users/me/messages")
                        .queryParam("q", "(subject:Java OR subject:Backend OR subject:Spring) newer_than:30d")
                        .queryParam("maxResults", 20)
                        .build())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .retrieve()
                .body(JsonNode.class);

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
}

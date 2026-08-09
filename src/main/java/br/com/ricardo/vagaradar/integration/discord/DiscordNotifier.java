package br.com.ricardo.vagaradar.integration.discord;

import br.com.ricardo.vagaradar.config.DiscordProperties;
import br.com.ricardo.vagaradar.entity.AnaliseVaga;
import br.com.ricardo.vagaradar.entity.Vaga;
import br.com.ricardo.vagaradar.exception.DiscordIntegrationException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class DiscordNotifier {

    private final RestClient restClient;
    private final DiscordProperties properties;

    public DiscordNotifier(RestClient.Builder restClientBuilder, DiscordProperties properties) {
        this.restClient = restClientBuilder.build();
        this.properties = properties;
    }

    public void notificarAnalise(Vaga vaga, AnaliseVaga analise) {
        if (properties.webhookUrl() == null || properties.webhookUrl().isBlank()
                || analise.getPontuacao() < properties.minimumScore()) {
            return;
        }

        try {
            restClient.post()
                    .uri(URI.create(properties.webhookUrl()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(montarMensagem(vaga, analise))
                    .retrieve()
                    .toBodilessEntity();
        } catch (IllegalArgumentException | RestClientException exception) {
            throw new DiscordIntegrationException("Não foi possível enviar o alerta para o Discord.", exception);
        }
    }

    private Map<String, Object> montarMensagem(Vaga vaga, AnaliseVaga analise) {
        Map<String, Object> embed = new LinkedHashMap<>();
        embed.put("title", limitar("Nova vaga: " + vaga.getCargo(), 256));
        embed.put("url", vaga.getLink());
        embed.put("color", corDaCompatibilidade(analise.getPontuacao()));
        embed.put("fields", List.of(
                campo("Empresa", limitar(vaga.getEmpresa(), 1_024), true),
                campo("Compatibilidade", "%d/100 · %s".formatted(
                        analise.getPontuacao(), analise.getNivelCompatibilidade()), true),
                campo("Recomendação", limitar(analise.getRecomendacao(), 1_024), false),
                campo("Link", limitar("[Abrir vaga](%s)".formatted(vaga.getLink()), 1_024), false)
        ));
        embed.put("footer", Map.of("text", "VagaRadar AI"));
        return Map.of(
                "embeds", List.of(embed),
                "allowed_mentions", Map.of("parse", List.of())
        );
    }

    private Map<String, Object> campo(String nome, String valor, boolean inline) {
        return Map.of("name", nome, "value", valor, "inline", inline);
    }

    private int corDaCompatibilidade(int pontuacao) {
        return pontuacao >= 85 ? 0x2ecc71 : pontuacao >= 70 ? 0x3498db : 0xf1c40f;
    }

    private String limitar(String value, int tamanhoMaximo) {
        String texto = value == null || value.isBlank() ? "Não informado" : value;
        if (texto.length() <= tamanhoMaximo) {
            return texto;
        }
        return texto.substring(0, tamanhoMaximo - 3) + "...";
    }
}

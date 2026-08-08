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
                    .body(Map.of("content", montarMensagem(vaga, analise)))
                    .retrieve()
                    .toBodilessEntity();
        } catch (IllegalArgumentException | RestClientException exception) {
            throw new DiscordIntegrationException("Não foi possível enviar o alerta para o Discord.", exception);
        }
    }

    private String montarMensagem(Vaga vaga, AnaliseVaga analise) {
        return """
                **Nova análise de vaga**
                **Cargo:** %s
                **Empresa:** %s
                **Compatibilidade:** %d/100 (%s)
                **Recomendação:** %s
                **Link:** %s
                """.formatted(
                vaga.getCargo(),
                vaga.getEmpresa(),
                analise.getPontuacao(),
                analise.getNivelCompatibilidade(),
                analise.getRecomendacao(),
                vaga.getLink()
        );
    }
}

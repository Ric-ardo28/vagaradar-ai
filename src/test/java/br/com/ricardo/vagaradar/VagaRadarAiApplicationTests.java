package br.com.ricardo.vagaradar;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import br.com.ricardo.vagaradar.repository.VagaRepository;
import br.com.ricardo.vagaradar.repository.AnaliseVagaRepository;
import br.com.ricardo.vagaradar.integration.openai.OpenAiVagaAnalyzer;
import br.com.ricardo.vagaradar.integration.discord.DiscordNotifier;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude="
                + "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration"
})
class VagaRadarAiApplicationTests {

    @MockitoBean
    private VagaRepository vagaRepository;

    @MockitoBean
    private AnaliseVagaRepository analiseVagaRepository;

    @MockitoBean
    private OpenAiVagaAnalyzer openAiVagaAnalyzer;

    @MockitoBean
    private DiscordNotifier discordNotifier;

    @Test
    void contextLoads() {
        // Verifica a inicialização básica da aplicação sem depender de um banco local.
    }
}

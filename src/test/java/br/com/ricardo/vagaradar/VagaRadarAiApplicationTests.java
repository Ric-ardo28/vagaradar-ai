package br.com.ricardo.vagaradar;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import br.com.ricardo.vagaradar.repository.VagaRepository;
import br.com.ricardo.vagaradar.repository.AnaliseVagaRepository;
import br.com.ricardo.vagaradar.integration.openai.OpenAiVagaAnalyzer;
import br.com.ricardo.vagaradar.integration.discord.DiscordNotifier;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude="
                + "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration",
        "gmail.oauth-enabled=false",
        "gmail.scheduler.enabled=false",
        "app.security.admin-username=test-admin",
        "app.security.admin-password=test-password-123"
})
@AutoConfigureMockMvc
class VagaRadarAiApplicationTests {

    @Autowired
    private MockMvc mockMvc;

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

    @Test
    void deveProtegerPainelEPermitirHealthCheck() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));

        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/vagas/1/descartar"))
                .andExpect(status().isForbidden());
    }

    @Test
    void deveAutenticarAdministradorComFormulario() throws Exception {
        mockMvc.perform(formLogin("/login")
                        .user("test-admin")
                        .password("test-password-123"))
                .andExpect(authenticated().withUsername("test-admin"));
    }
}

package br.com.ricardo.vagaradar;

import br.com.ricardo.vagaradar.entity.ModeloTrabalho;
import br.com.ricardo.vagaradar.entity.Vaga;
import br.com.ricardo.vagaradar.dto.VagaResponse;
import br.com.ricardo.vagaradar.repository.VagaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "gmail.oauth-enabled=false",
                "gmail.scheduler.enabled=false",
                "openai.api-key=",
                "discord.webhook-url=",
                "app.security.admin-username=test-admin",
                "app.security.admin-password=test-password-123"
        }
)
class VagaRadarPostgresIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("vagaradar")
            .withUsername("vagaradar")
            .withPassword("vagaradar");

    @DynamicPropertySource
    static void configurarBanco(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private VagaRepository vagaRepository;

    @Autowired
    private TestRestTemplate restTemplate;

    @LocalServerPort
    private int port;

    @Test
    void deveAplicarMigracoesEPersistirVagaNoPostgres() {
        Vaga vaga = vagaRepository.save(new Vaga(
                "integration-1",
                "Desenvolvedor Java Júnior",
                "Empresa de Teste",
                "Java, Spring Boot e PostgreSQL",
                "São Paulo",
                ModeloTrabalho.HIBRIDO,
                "https://www.linkedin.com/jobs/view/integration-1",
                null
        ));

        assertThat(vaga.getId()).isNotNull();
        assertThat(vagaRepository.findByLink(vaga.getLink())).isPresent();
    }

    @Test
    void deveExporHealthCheckComBancoDisponivel() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/actuator/health",
                String.class
        );

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).contains("UP");
    }

    @Test
    void deveBuscarPaginaSemFiltroNoPostgres() {
        Page<Vaga> pagina = vagaRepository.buscarPaginado(
                null, null, null, null, null, PageRequest.of(0, 25)
        );

        assertThat(pagina.getSize()).isEqualTo(25);
    }
}

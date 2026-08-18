package br.com.ricardo.vagaradar.controller;

import br.com.ricardo.vagaradar.dto.VagaResponse;
import br.com.ricardo.vagaradar.dto.PaginaVagasResponse;
import br.com.ricardo.vagaradar.entity.ModeloTrabalho;
import br.com.ricardo.vagaradar.entity.StatusVaga;
import br.com.ricardo.vagaradar.service.VagaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(VagaController.class)
@AutoConfigureMockMvc(addFilters = false)
class VagaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private VagaService vagaService;

    @Test
    void deveListarVagas() throws Exception {
        VagaResponse vaga = new VagaResponse(
                1L, "abc123", "Desenvolvedor Java", "Empresa X", "Java e Spring Boot",
                "São Paulo", ModeloTrabalho.HIBRIDO, "https://www.linkedin.com/jobs/view/123",
                Instant.parse("2026-08-07T12:00:00Z"), Instant.parse("2026-08-07T13:00:00Z"),
                Instant.parse("2026-08-07T14:00:00Z"), StatusVaga.RECEBIDA, null
        );
        given(vagaService.listarPaginado(0, null, null, null, null))
                .willReturn(new PaginaVagasResponse(List.of(vaga), 0, 25, 1, 1, 1, 0, 1));

        mockMvc.perform(get("/api/vagas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vagas[0].id").value(1))
                .andExpect(jsonPath("$.vagas[0].cargo").value("Desenvolvedor Java"))
                .andExpect(jsonPath("$.vagas[0].analisadaEm").value("2026-08-07T14:00:00Z"))
                .andExpect(jsonPath("$.tamanho").value(25));
    }

    @Test
    void deveRejeitarCadastroComCamposObrigatoriosAusentes() throws Exception {
        mockMvc.perform(post("/api/vagas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"empresa\":\"Empresa X\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deveRetornarErroPadronizadoQuandoVagaNaoExiste() throws Exception {
        given(vagaService.buscarPorId(99L))
                .willThrow(new br.com.ricardo.vagaradar.exception.VagaNaoEncontradaException(99L));

        mockMvc.perform(get("/api/vagas/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Vaga não encontrada para o identificador: 99"))
                .andExpect(jsonPath("$.path").value("/api/vagas/99"));
    }

    @Test
    void deveRetornarConflitoQuandoVagaEstaDuplicada() throws Exception {
        given(vagaService.criar(org.mockito.ArgumentMatchers.any()))
                .willThrow(new br.com.ricardo.vagaradar.exception.VagaDuplicadaException());

        mockMvc.perform(post("/api/vagas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"cargo":"Desenvolvedor Java","empresa":"Empresa X","descricao":"Java e Spring",
                                "link":"https://www.linkedin.com/jobs/view/123"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Esta vaga já foi cadastrada."));
    }

    @Test
    void deveDescartarVaga() throws Exception {
        VagaResponse descartada = new VagaResponse(
                1L, "abc123", "Desenvolvedor Java", "Empresa X", "Java e Spring Boot",
                "São Paulo", ModeloTrabalho.HIBRIDO, "https://www.linkedin.com/jobs/view/123",
                null, Instant.parse("2026-08-07T13:00:00Z"), null, StatusVaga.DESCARTADA, null
        );
        given(vagaService.descartar(1L)).willReturn(descartada);

        mockMvc.perform(post("/api/vagas/1/descartar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DESCARTADA"));
    }
}

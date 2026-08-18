package br.com.ricardo.vagaradar.controller;

import br.com.ricardo.vagaradar.dto.AnaliseVagaResponse;
import br.com.ricardo.vagaradar.dto.VagaResponse;
import br.com.ricardo.vagaradar.entity.ModeloTrabalho;
import br.com.ricardo.vagaradar.entity.NivelCompatibilidade;
import br.com.ricardo.vagaradar.entity.StatusVaga;
import br.com.ricardo.vagaradar.service.VagaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ExtensionVagaController.class)
@AutoConfigureMockMvc(addFilters = false)
class ExtensionVagaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private VagaService vagaService;

    @Test
    void deveCriarEAnalisarVagaEnviadaPelaExtensao() throws Exception {
        VagaResponse vaga = new VagaResponse(8L, "123", "Java Júnior", "Empresa", "Java e Spring", "São Paulo",
                ModeloTrabalho.HIBRIDO, "https://www.linkedin.com/jobs/view/123", null, Instant.now(), null, StatusVaga.RECEBIDA, null);
        AnaliseVagaResponse analise = new AnaliseVagaResponse(4L, 8L, 82, NivelCompatibilidade.ALTA,
                "Java", "Cloud", "Candidatar", Instant.now());
        given(vagaService.criar(any())).willReturn(vaga);
        given(vagaService.analisar(8L)).willReturn(analise);

        mockMvc.perform(post("/api/extensao/vagas")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"linkedinId":"123","cargo":"Java Júnior","empresa":"Empresa",
                                "descricao":"Java e Spring","localizacao":"São Paulo","modeloTrabalho":"HIBRIDO",
                                "link":"https://www.linkedin.com/jobs/view/123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vaga.id").value(8))
                .andExpect(jsonPath("$.analise.pontuacao").value(82));
    }
}

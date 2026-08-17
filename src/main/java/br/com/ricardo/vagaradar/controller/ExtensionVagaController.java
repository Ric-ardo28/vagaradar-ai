package br.com.ricardo.vagaradar.controller;

import br.com.ricardo.vagaradar.dto.AnaliseVagaResponse;
import br.com.ricardo.vagaradar.dto.ExtensionVagaResponse;
import br.com.ricardo.vagaradar.dto.VagaCreateRequest;
import br.com.ricardo.vagaradar.dto.VagaResponse;
import br.com.ricardo.vagaradar.service.VagaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/extensao/vagas")
public class ExtensionVagaController {

    private final VagaService vagaService;

    public ExtensionVagaController(VagaService vagaService) {
        this.vagaService = vagaService;
    }

    @PostMapping
    public ExtensionVagaResponse criarEAnalisar(@Valid @RequestBody VagaCreateRequest request) {
        VagaResponse vaga = vagaService.criar(request);
        AnaliseVagaResponse analise = vagaService.analisar(vaga.id());
        return new ExtensionVagaResponse(vaga, analise);
    }
}

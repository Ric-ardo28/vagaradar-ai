package br.com.ricardo.vagaradar.controller;

import br.com.ricardo.vagaradar.dto.VagaCreateRequest;
import br.com.ricardo.vagaradar.dto.VagaResponse;
import br.com.ricardo.vagaradar.dto.AnaliseVagaResponse;
import br.com.ricardo.vagaradar.service.VagaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/vagas")
public class VagaController {

    private final VagaService vagaService;

    public VagaController(VagaService vagaService) {
        this.vagaService = vagaService;
    }

    @PostMapping
    public ResponseEntity<VagaResponse> criar(@Valid @RequestBody VagaCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(vagaService.criar(request));
    }

    @GetMapping
    public List<VagaResponse> listar() {
        return vagaService.listar();
    }

    @GetMapping("/{id}")
    public VagaResponse buscarPorId(@PathVariable Long id) {
        return vagaService.buscarPorId(id);
    }

    @PostMapping("/{id}/analise")
    public AnaliseVagaResponse analisar(@PathVariable Long id) {
        return vagaService.analisar(id);
    }
}

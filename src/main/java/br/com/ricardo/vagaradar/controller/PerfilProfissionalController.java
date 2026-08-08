package br.com.ricardo.vagaradar.controller;

import br.com.ricardo.vagaradar.dto.PerfilProfissionalRequest;
import br.com.ricardo.vagaradar.dto.PerfilProfissionalResponse;
import br.com.ricardo.vagaradar.service.PerfilProfissionalService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/perfil")
public class PerfilProfissionalController {

    private final PerfilProfissionalService perfilProfissionalService;

    public PerfilProfissionalController(PerfilProfissionalService perfilProfissionalService) {
        this.perfilProfissionalService = perfilProfissionalService;
    }

    @GetMapping
    public PerfilProfissionalResponse obter() {
        return perfilProfissionalService.obter();
    }

    @PutMapping
    public PerfilProfissionalResponse atualizar(@Valid @RequestBody PerfilProfissionalRequest request) {
        return perfilProfissionalService.atualizar(request);
    }

    @DeleteMapping
    public ResponseEntity<Void> restaurarPadrao() {
        perfilProfissionalService.restaurarPadrao();
        return ResponseEntity.noContent().build();
    }
}

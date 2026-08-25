package br.com.ricardo.vagaradar.controller;

import br.com.ricardo.vagaradar.dto.VagaCreateRequest;
import br.com.ricardo.vagaradar.dto.VagaResponse;
import br.com.ricardo.vagaradar.dto.AnaliseVagaResponse;
import br.com.ricardo.vagaradar.dto.AvaliacaoVagaRequest;
import br.com.ricardo.vagaradar.entity.AvaliacaoUsuario;
import br.com.ricardo.vagaradar.dto.PaginaVagasResponse;
import br.com.ricardo.vagaradar.entity.ModeloTrabalho;
import br.com.ricardo.vagaradar.entity.StatusVaga;
import br.com.ricardo.vagaradar.service.VagaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
    public PaginaVagasResponse listar(
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(required = false) String busca,
            @RequestParam(required = false) ModeloTrabalho modeloTrabalho,
            @RequestParam(required = false) StatusVaga status,
            @RequestParam(required = false) AvaliacaoUsuario avaliacaoUsuario,
            @RequestParam(required = false) Integer notaMinima
    ) {
        return vagaService.listarPaginado(Math.max(pagina, 0), busca, modeloTrabalho, status, avaliacaoUsuario, notaMinima);
    }

    @GetMapping("/{id}")
    public VagaResponse buscarPorId(@PathVariable Long id) {
        return vagaService.buscarPorId(id);
    }

    @PostMapping("/{id}/analise")
    public AnaliseVagaResponse analisar(@PathVariable Long id) {
        return vagaService.analisar(id);
    }

    @PostMapping("/{id}/descartar")
    public VagaResponse descartar(@PathVariable Long id) {
        return vagaService.descartar(id);
    }

    @PostMapping("/{id}/avaliacao")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void avaliar(@PathVariable Long id, @Valid @RequestBody AvaliacaoVagaRequest request) {
        vagaService.avaliar(id, request);
    }
}

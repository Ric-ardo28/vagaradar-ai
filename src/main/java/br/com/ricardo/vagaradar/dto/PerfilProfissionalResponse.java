package br.com.ricardo.vagaradar.dto;

import java.time.Instant;

public record PerfilProfissionalResponse(
        String objetivo,
        String stackPrincipal,
        String conhecimentosBasicos,
        String formacao,
        String experiencia,
        String preferencias,
        boolean personalizado,
        Instant atualizadoEm
) {
}

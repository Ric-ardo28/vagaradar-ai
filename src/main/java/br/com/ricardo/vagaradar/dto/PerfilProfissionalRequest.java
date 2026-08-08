package br.com.ricardo.vagaradar.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PerfilProfissionalRequest(
        @NotBlank @Size(max = 3000) String objetivo,
        @NotBlank @Size(max = 3000) String stackPrincipal,
        @NotBlank @Size(max = 3000) String conhecimentosBasicos,
        @NotBlank @Size(max = 3000) String formacao,
        @NotBlank @Size(max = 3000) String experiencia,
        @NotBlank @Size(max = 3000) String preferencias
) {
}

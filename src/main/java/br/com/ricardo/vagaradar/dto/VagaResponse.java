package br.com.ricardo.vagaradar.dto;

import br.com.ricardo.vagaradar.entity.ModeloTrabalho;
import br.com.ricardo.vagaradar.entity.StatusVaga;

import java.time.Instant;

public record VagaResponse(
        Long id,
        String linkedinId,
        String cargo,
        String empresa,
        String descricao,
        String localizacao,
        ModeloTrabalho modeloTrabalho,
        String link,
        Instant dataPublicacao,
        Instant dataEncontrada,
        StatusVaga status
) {
}

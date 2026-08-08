package br.com.ricardo.vagaradar.dto;

import br.com.ricardo.vagaradar.entity.NivelCompatibilidade;

import java.time.Instant;

public record AnaliseVagaResponse(
        Long id,
        Long vagaId,
        Integer pontuacao,
        NivelCompatibilidade nivelCompatibilidade,
        String pontosFortes,
        String pontosFaltantes,
        String recomendacao,
        Instant analisadaEm
) {
}

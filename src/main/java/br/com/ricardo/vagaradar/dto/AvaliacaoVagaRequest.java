package br.com.ricardo.vagaradar.dto;

import br.com.ricardo.vagaradar.entity.AvaliacaoUsuario;
import br.com.ricardo.vagaradar.entity.MotivoRejeicao;
import jakarta.validation.constraints.NotNull;

import java.util.Set;

public record AvaliacaoVagaRequest(
        @NotNull AvaliacaoUsuario avaliacao,
        Set<MotivoRejeicao> motivosRejeicao,
        String outroMotivo
) {
}

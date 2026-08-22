package br.com.ricardo.vagaradar.dto;

import br.com.ricardo.vagaradar.entity.ModeloTrabalho;
import br.com.ricardo.vagaradar.entity.StatusVaga;
import br.com.ricardo.vagaradar.entity.AvaliacaoUsuario;
import br.com.ricardo.vagaradar.entity.MotivoRejeicao;

import java.time.Instant;
import java.util.Set;

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
        Instant analisadaEm,
        StatusVaga status,
        Integer pontuacao,
        AvaliacaoUsuario avaliacaoUsuario,
        Set<MotivoRejeicao> motivosRejeicao,
        String outroMotivoRejeicao
) {
}

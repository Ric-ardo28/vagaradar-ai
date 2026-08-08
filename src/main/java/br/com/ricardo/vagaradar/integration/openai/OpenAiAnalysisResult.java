package br.com.ricardo.vagaradar.integration.openai;

import br.com.ricardo.vagaradar.entity.NivelCompatibilidade;

public record OpenAiAnalysisResult(
        Integer pontuacao,
        NivelCompatibilidade nivelCompatibilidade,
        String pontosFortes,
        String pontosFaltantes,
        String recomendacao
) {
}

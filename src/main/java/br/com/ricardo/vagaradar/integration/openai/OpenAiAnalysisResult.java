package br.com.ricardo.vagaradar.integration.openai;

import br.com.ricardo.vagaradar.entity.NivelCompatibilidade;
import br.com.ricardo.vagaradar.entity.ModeloTrabalho;

import java.util.List;

public record OpenAiAnalysisResult(
        Integer pontuacao,
        NivelCompatibilidade nivelCompatibilidade,
        String pontosFortes,
        String pontosFaltantes,
        String recomendacao,
        boolean dadosExtraidosConfiaveis,
        String localizacaoExtraida,
        ModeloTrabalho modeloTrabalhoExtraido,
        List<String> tecnologias,
        List<String> habilidades,
        String senioridade,
        String requisitosPrincipais
) {
}

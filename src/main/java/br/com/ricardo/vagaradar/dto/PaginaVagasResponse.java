package br.com.ricardo.vagaradar.dto;

import java.util.List;

public record PaginaVagasResponse(
        List<VagaResponse> vagas,
        int pagina,
        int tamanho,
        long totalElementos,
        int totalPaginas,
        long totalMonitoradas,
        long totalAnalisadas,
        long totalPendentes,
        long totalPendentesAvaliacao,
        long totalGostei,
        long totalNaoGostei
) {
}

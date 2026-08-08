package br.com.ricardo.vagaradar.integration.gmail;

import java.util.List;

public record GmailImportResult(
        int mensagensLidas,
        int vagasImportadas,
        int vagasIgnoradas,
        List<Long> vagasImportadasIds
) {
}

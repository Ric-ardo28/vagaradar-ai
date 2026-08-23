package br.com.ricardo.vagaradar.integration.gmail;

import br.com.ricardo.vagaradar.dto.VagaCreateRequest;
import br.com.ricardo.vagaradar.entity.ModeloTrabalho;
import org.springframework.web.util.HtmlUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GmailJobAlertParser {

    private static final int MAX_DESCRIPTION_LENGTH = 8_000;

    private static final Pattern ANCHOR_PATTERN = Pattern.compile(
            "<a\\b[^>]*?href\\s*=\\s*[\\\"'](?<url>[^\\\"']+)[\\\"'][^>]*>(?<label>.*?)</a>",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    private static final Pattern LINKEDIN_JOB_URL_PATTERN = Pattern.compile(
            "https?://[^\\s\\\"'<>]+linkedin\\.com/(?:comm/)?jobs/view/(?<jobId>\\d+)[^\\s\\\"'<>]*",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern DATA_RELATIVA_PATTERN = Pattern.compile(
            "(?i)h[áa]\\s+(?<quantidade>\\d+)\\s+(?<unidade>hora|horas|dia|dias|semana|semanas|m[eê]s|meses)"
    );
    private static final Pattern DATA_EXATA_PATTERN = Pattern.compile(
            "(?i)publicad[ao]\\s+(?:em\\s+)?(?<dia>\\d{1,2})[/-](?<mes>\\d{1,2})[/-](?<ano>\\d{4})"
    );

    public List<VagaCreateRequest> extrairVagas(GmailJobAlert alert) {
        if (ehConfirmacaoDeAlerta(alert.subject())) {
            return List.of();
        }
        Map<String, VagaExtraida> vagasPorLink = new LinkedHashMap<>();
        extrairLinksDoHtml(alert.html(), vagasPorLink);
        extrairLinksDoTexto(alert, vagasPorLink);

        return vagasPorLink.entrySet().stream()
                .filter(entry -> temTituloDeVaga(entry.getValue().titulo()))
                .map(entry -> criarVaga(alert, entry.getKey(), entry.getValue(), vagasPorLink.size()))
                .toList();
    }

    private boolean ehConfirmacaoDeAlerta(String subject) {
        return subject != null && subject.toLowerCase().contains("foi criado seu alerta de vaga");
    }

    private void extrairLinksDoHtml(String html, Map<String, VagaExtraida> vagasPorLink) {
        if (html == null || html.isBlank()) {
            return;
        }

        Matcher matcher = ANCHOR_PATTERN.matcher(html);
        while (matcher.find()) {
            String link = normalizarLink(matcher.group("url"));
            if (link != null) {
                String titulo = limparTexto(matcher.group("label"));
                vagasPorLink.putIfAbsent(link, new VagaExtraida(titulo, null, ""));
            }
        }
    }

    private void extrairLinksDoTexto(GmailJobAlert alert, Map<String, VagaExtraida> vagasPorLink) {
        String text = alert.plainText();
        if (text == null || text.isBlank()) {
            return;
        }

        String textoDoAlerta = HtmlUtils.htmlUnescape(text);
        Matcher matcher = LINKEDIN_JOB_URL_PATTERN.matcher(textoDoAlerta);
        int fimDoLinkAnterior = 0;
        while (matcher.find()) {
            String link = normalizarLink(matcher.group());
            if (link != null) {
                String trechoDaVaga = textoDoAlerta.substring(fimDoLinkAnterior, matcher.start());
                String titulo = extrairTituloAntesDoLink(trechoDaVaga);
                VagaExtraida vagaExistente = vagasPorLink.get(link);
                if (!titulo.isBlank()) {
                    vagasPorLink.put(link, new VagaExtraida(titulo, extrairDataPublicacao(trechoDaVaga, alert.receivedAt()), limparTexto(trechoDaVaga)));
                } else {
                    vagasPorLink.putIfAbsent(link, vagaExistente == null ? new VagaExtraida("", null, "") : vagaExistente);
                }
            }
            fimDoLinkAnterior = matcher.end();
        }
    }

    private String extrairTituloAntesDoLink(String trecho) {
        String textoAntesDoLink = trecho
                .replaceAll("(?i)visualizar\\s+vaga\\s*:\\s*$", "")
                .trim();

        for (String linha : textoAntesDoLink.split("\\R")) {
            String candidata = limparTexto(linha);
            if (!candidata.isBlank() && !ehTextoEstruturalDoAlerta(candidata)) {
                return candidata;
            }
        }
        return "";
    }

    private boolean ehTextoEstruturalDoAlerta(String value) {
        String texto = value.toLowerCase();
        return texto.matches("[-—_ ]+")
                || texto.startsWith("seu alerta de vaga")
                || texto.startsWith("novas vagas correspondem")
                || texto.startsWith("candidate-se com")
                || texto.startsWith("alerta do linkedin:");
    }

    private boolean temTituloDeVaga(String value) {
        String titulo = limparTexto(value);
        if (titulo.isBlank() || titulo.length() < 3 || titulo.matches("(?i)https?://.*")) {
            return false;
        }

        String texto = titulo.toLowerCase();
        return !texto.matches("\\d+\\s+vagas?\\s+novas?\\s+correspondem\\s+[àa]s\\s+suas\\s+prefer[eê]ncias\\.?")
                && !texto.startsWith("ver todas as vagas")
                && !texto.startsWith("ver vagas no linkedin")
                && !texto.startsWith("visualizar vaga")
                && !texto.startsWith("candidate-se com")
                && !texto.startsWith("alerta de vagas")
                && !texto.startsWith("seu alerta de vaga");
    }

    private VagaCreateRequest criarVaga(GmailJobAlert alert, String link, VagaExtraida vagaExtraida, int totalDeVagas) {
        String descricao = descricaoIsoladaDaVaga(alert, vagaExtraida, totalDeVagas);

        return new VagaCreateRequest(
                null,
                limitar(vagaExtraida.titulo(), 255),
                "Não informado (alerta do LinkedIn)",
                descricao,
                null,
                ModeloTrabalho.NAO_INFORMADO,
                link,
                vagaExtraida.dataPublicacao()
        );
    }

    private java.time.Instant extrairDataPublicacao(String trechoDaVaga, java.time.Instant dataRecebimento) {
        Matcher dataExata = DATA_EXATA_PATTERN.matcher(trechoDaVaga);
        if (dataExata.find()) {
            return LocalDate.of(
                    Integer.parseInt(dataExata.group("ano")),
                    Integer.parseInt(dataExata.group("mes")),
                    Integer.parseInt(dataExata.group("dia"))
            ).atStartOfDay().toInstant(ZoneOffset.UTC);
        }

        Matcher dataRelativa = DATA_RELATIVA_PATTERN.matcher(trechoDaVaga);
        if (!dataRelativa.find()) {
            return null;
        }

        long quantidade = Long.parseLong(dataRelativa.group("quantidade"));
        String unidade = dataRelativa.group("unidade").toLowerCase();
        if (unidade.startsWith("hora")) {
            return dataRecebimento.minus(quantidade, ChronoUnit.HOURS);
        }
        if (unidade.startsWith("dia")) {
            return dataRecebimento.minus(quantidade, ChronoUnit.DAYS);
        }
        if (unidade.startsWith("semana")) {
            return dataRecebimento.minus(quantidade, ChronoUnit.WEEKS);
        }
        return dataRecebimento.atZone(ZoneOffset.UTC).minusMonths(quantidade).toInstant();
    }

    private String normalizarLink(String value) {
        if (value == null) {
            return null;
        }

        String link = HtmlUtils.htmlUnescape(value).trim().replaceAll("[),.;]+$", "");
        Matcher matcher = LINKEDIN_JOB_URL_PATTERN.matcher(link);
        if (!matcher.matches()) {
            return null;
        }
        return "https://www.linkedin.com/jobs/view/" + matcher.group("jobId");
    }

    private String descricaoIsoladaDaVaga(GmailJobAlert alert, VagaExtraida vagaExtraida, int totalDeVagas) {
        if (!vagaExtraida.trechoConfiavel().isBlank()) {
            return limitar("Trecho do alerta para esta vaga: " + vagaExtraida.trechoConfiavel(), MAX_DESCRIPTION_LENGTH);
        }
        if (totalDeVagas > 1) {
            return "Título da vaga: " + limitar(vagaExtraida.titulo(), 255);
        }
        return descricaoDoAlertaUnico(alert);
    }

    private String descricaoDoAlertaUnico(GmailJobAlert alert) {
        String corpo = limparTexto(alert.plainText());
        if (corpo.isBlank()) {
            corpo = limparTexto(alert.html());
        }
        if (corpo.isBlank()) {
            corpo = "Alerta de vaga recebido pelo Gmail.";
        }

        return limitar("Alerta do LinkedIn: " + corpo, MAX_DESCRIPTION_LENGTH);
    }

    private String limparTexto(String value) {
        if (value == null) {
            return "";
        }
        return HtmlUtils.htmlUnescape(value.replaceAll("(?is)<[^>]+>", " "))
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String limitar(String value, int tamanhoMaximo) {
        return value.length() <= tamanhoMaximo ? value : value.substring(0, tamanhoMaximo);
    }

    private record VagaExtraida(String titulo, java.time.Instant dataPublicacao, String trechoConfiavel) {
    }
}

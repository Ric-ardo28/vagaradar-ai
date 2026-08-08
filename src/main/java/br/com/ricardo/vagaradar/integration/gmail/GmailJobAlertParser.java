package br.com.ricardo.vagaradar.integration.gmail;

import br.com.ricardo.vagaradar.dto.VagaCreateRequest;
import br.com.ricardo.vagaradar.entity.ModeloTrabalho;
import org.springframework.web.util.HtmlUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GmailJobAlertParser {

    private static final Pattern ANCHOR_PATTERN = Pattern.compile(
            "<a\\b[^>]*?href\\s*=\\s*[\\\"'](?<url>[^\\\"']+)[\\\"'][^>]*>(?<label>.*?)</a>",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    private static final Pattern LINKEDIN_JOB_URL_PATTERN = Pattern.compile(
            "https?://[^\\s\\\"'<>]+linkedin\\.com/(?:comm/)?jobs/view/\\d+[^\\s\\\"'<>]*",
            Pattern.CASE_INSENSITIVE
    );

    public List<VagaCreateRequest> extrairVagas(GmailJobAlert alert) {
        Map<String, String> vagasPorLink = new LinkedHashMap<>();
        extrairLinksDoHtml(alert.html(), vagasPorLink);
        extrairLinksDoTexto(alert.plainText(), vagasPorLink);

        return vagasPorLink.entrySet().stream()
                .map(entry -> criarVaga(alert, entry.getKey(), entry.getValue()))
                .toList();
    }

    private void extrairLinksDoHtml(String html, Map<String, String> vagasPorLink) {
        if (html == null || html.isBlank()) {
            return;
        }

        Matcher matcher = ANCHOR_PATTERN.matcher(html);
        while (matcher.find()) {
            String link = normalizarLink(matcher.group("url"));
            if (link != null) {
                String titulo = limparTexto(matcher.group("label"));
                vagasPorLink.putIfAbsent(link, titulo);
            }
        }
    }

    private void extrairLinksDoTexto(String text, Map<String, String> vagasPorLink) {
        if (text == null || text.isBlank()) {
            return;
        }

        Matcher matcher = LINKEDIN_JOB_URL_PATTERN.matcher(HtmlUtils.htmlUnescape(text));
        while (matcher.find()) {
            String link = normalizarLink(matcher.group());
            if (link != null) {
                vagasPorLink.putIfAbsent(link, "");
            }
        }
    }

    private VagaCreateRequest criarVaga(GmailJobAlert alert, String link, String tituloDoLink) {
        String cargo = tituloDoLink == null || tituloDoLink.isBlank()
                ? cargoDoAssunto(alert.subject())
                : tituloDoLink;
        String descricao = descricaoDoAlerta(alert);

        return new VagaCreateRequest(
                null,
                limitar(cargo, 255),
                "Não informado (alerta do LinkedIn)",
                descricao,
                null,
                ModeloTrabalho.NAO_INFORMADO,
                link,
                alert.receivedAt()
        );
    }

    private String normalizarLink(String value) {
        if (value == null) {
            return null;
        }

        String link = HtmlUtils.htmlUnescape(value).trim().replaceAll("[),.;]+$", "");
        return LINKEDIN_JOB_URL_PATTERN.matcher(link).matches() ? link : null;
    }

    private String cargoDoAssunto(String subject) {
        String resultado = limparTexto(subject);
        if (resultado.isBlank()) {
            return "Vaga encontrada no alerta do LinkedIn";
        }
        return resultado;
    }

    private String descricaoDoAlerta(GmailJobAlert alert) {
        String corpo = limparTexto(alert.plainText());
        if (corpo.isBlank()) {
            corpo = limparTexto(alert.html());
        }
        if (corpo.isBlank()) {
            corpo = "Alerta de vaga recebido pelo Gmail.";
        }

        return limitar("Alerta do LinkedIn: " + corpo, 20_000);
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
}

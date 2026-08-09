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

    private static final int MAX_DESCRIPTION_LENGTH = 8_000;

    private static final Pattern ANCHOR_PATTERN = Pattern.compile(
            "<a\\b[^>]*?href\\s*=\\s*[\\\"'](?<url>[^\\\"']+)[\\\"'][^>]*>(?<label>.*?)</a>",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    private static final Pattern LINKEDIN_JOB_URL_PATTERN = Pattern.compile(
            "https?://[^\\s\\\"'<>]+linkedin\\.com/(?:comm/)?jobs/view/(?<jobId>\\d+)[^\\s\\\"'<>]*",
            Pattern.CASE_INSENSITIVE
    );

    public List<VagaCreateRequest> extrairVagas(GmailJobAlert alert) {
        if (ehConfirmacaoDeAlerta(alert.subject())) {
            return List.of();
        }
        Map<String, String> vagasPorLink = new LinkedHashMap<>();
        extrairLinksDoHtml(alert.html(), vagasPorLink);
        extrairLinksDoTexto(alert.plainText(), vagasPorLink);

        return vagasPorLink.entrySet().stream()
                .map(entry -> criarVaga(alert, entry.getKey(), entry.getValue()))
                .toList();
    }

    private boolean ehConfirmacaoDeAlerta(String subject) {
        return subject != null && subject.toLowerCase().contains("foi criado seu alerta de vaga");
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

        String textoDoAlerta = HtmlUtils.htmlUnescape(text);
        Matcher matcher = LINKEDIN_JOB_URL_PATTERN.matcher(textoDoAlerta);
        int fimDoLinkAnterior = 0;
        while (matcher.find()) {
            String link = normalizarLink(matcher.group());
            if (link != null) {
                String titulo = extrairTituloAntesDoLink(textoDoAlerta.substring(fimDoLinkAnterior, matcher.start()));
                if (!titulo.isBlank()) {
                    vagasPorLink.put(link, titulo);
                } else {
                    vagasPorLink.putIfAbsent(link, "");
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
        Matcher matcher = LINKEDIN_JOB_URL_PATTERN.matcher(link);
        if (!matcher.matches()) {
            return null;
        }
        return "https://www.linkedin.com/jobs/view/" + matcher.group("jobId");
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
}

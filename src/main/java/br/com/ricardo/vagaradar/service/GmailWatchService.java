package br.com.ricardo.vagaradar.service;

import br.com.ricardo.vagaradar.config.GmailPushProperties;
import br.com.ricardo.vagaradar.integration.gmail.GmailReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(prefix = "gmail.push", name = "enabled", havingValue = "true")
public class GmailWatchService {

    private static final Logger LOGGER = LoggerFactory.getLogger(GmailWatchService.class);

    private final GmailPushProperties properties;
    private final GmailAuthorizedProcessingService authorizedProcessingService;
    private final GmailReader gmailReader;

    public GmailWatchService(
            GmailPushProperties properties,
            GmailAuthorizedProcessingService authorizedProcessingService,
            GmailReader gmailReader
    ) {
        this.properties = properties;
        this.authorizedProcessingService = authorizedProcessingService;
        this.gmailReader = gmailReader;
    }

    public void renovarMonitoramento() {
        validarConfiguracao();
        authorizedProcessingService.buscarClienteAutorizado().ifPresentOrElse(client -> {
            var watch = gmailReader.iniciarMonitoramentoDaCaixaDeEntrada(
                    client.getAccessToken().getTokenValue(), properties.topicName()
            );
            LOGGER.info("Monitoramento do Gmail renovado até {}.", watch.expirationEpochMillis());
        }, () -> LOGGER.warn("Monitoramento do Gmail ignorado: nenhuma conta autorizada."));
    }

    private void validarConfiguracao() {
        if (emBranco(properties.topicName()) || emBranco(properties.audience()) || emBranco(properties.serviceAccountEmail())) {
            throw new IllegalStateException(
                    "Defina GMAIL_PUSH_TOPIC_NAME, GMAIL_PUSH_AUDIENCE e GMAIL_PUSH_SERVICE_ACCOUNT_EMAIL."
            );
        }
    }

    private boolean emBranco(String valor) {
        return valor == null || valor.isBlank();
    }
}

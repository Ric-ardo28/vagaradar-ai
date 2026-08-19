package br.com.ricardo.vagaradar.scheduler;

import br.com.ricardo.vagaradar.service.GmailAuthorizedProcessingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "gmail.scheduler", name = "enabled", havingValue = "true")
public class GmailProcessingScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GmailProcessingScheduler.class);

    private final GmailAuthorizedProcessingService authorizedProcessingService;

    public GmailProcessingScheduler(
            GmailAuthorizedProcessingService authorizedProcessingService
    ) {
        this.authorizedProcessingService = authorizedProcessingService;
    }

    @Scheduled(
            fixedDelayString = "${gmail.scheduler.fixed-delay:PT6H}",
            initialDelayString = "${gmail.scheduler.initial-delay:PT5M}"
    )
    public void processarNovosAlertas() {
        authorizedProcessingService.processarContaAutorizada().ifPresentOrElse(result ->
            LOGGER.info(
                    "Agendamento do Gmail concluído: {} mensagens, {} vagas importadas e {} analisadas.",
                    result.importacao().mensagensLidas(),
                    result.importacao().vagasImportadas(),
                    result.vagasAnalisadas()
            ), () -> LOGGER.warn("Agendamento do Gmail ignorado ou falhou."));
    }
}

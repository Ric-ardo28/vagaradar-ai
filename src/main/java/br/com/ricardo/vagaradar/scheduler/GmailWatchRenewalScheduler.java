package br.com.ricardo.vagaradar.scheduler;

import br.com.ricardo.vagaradar.service.GmailWatchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "gmail.push", name = "enabled", havingValue = "true")
public class GmailWatchRenewalScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GmailWatchRenewalScheduler.class);
    private final GmailWatchService gmailWatchService;

    public GmailWatchRenewalScheduler(GmailWatchService gmailWatchService) {
        this.gmailWatchService = gmailWatchService;
    }

    @Scheduled(
            fixedDelayString = "${gmail.push.renewal-fixed-delay:P1D}",
            initialDelayString = "${gmail.push.renewal-initial-delay:PT2M}"
    )
    public void renovarMonitoramento() {
        try {
            gmailWatchService.renovarMonitoramento();
        } catch (RuntimeException exception) {
            LOGGER.error("Não foi possível renovar o monitoramento do Gmail.", exception);
        }
    }
}

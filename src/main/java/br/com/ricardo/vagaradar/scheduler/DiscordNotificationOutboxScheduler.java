package br.com.ricardo.vagaradar.scheduler;

import br.com.ricardo.vagaradar.service.DiscordNotificationOutboxService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "discord.outbox", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DiscordNotificationOutboxScheduler {

    private final DiscordNotificationOutboxService outboxService;

    public DiscordNotificationOutboxScheduler(DiscordNotificationOutboxService outboxService) {
        this.outboxService = outboxService;
    }

    @Scheduled(fixedDelayString = "${discord.outbox.fixed-delay:PT30S}", initialDelayString = "${discord.outbox.initial-delay:PT10S}")
    public void enviarPendentes() {
        outboxService.processarPendentes();
    }
}

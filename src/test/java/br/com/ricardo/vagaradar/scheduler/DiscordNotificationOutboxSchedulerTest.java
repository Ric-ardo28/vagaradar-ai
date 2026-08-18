package br.com.ricardo.vagaradar.scheduler;

import br.com.ricardo.vagaradar.service.DiscordNotificationOutboxService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DiscordNotificationOutboxSchedulerTest {

    @Mock
    private DiscordNotificationOutboxService outboxService;

    @Test
    void deveProcessarPendentesPeloMetodoTransacional() {
        when(outboxService.processarProxima()).thenReturn(true, true, false);

        new DiscordNotificationOutboxScheduler(outboxService).enviarPendentes();

        verify(outboxService, times(3)).processarProxima();
    }
}

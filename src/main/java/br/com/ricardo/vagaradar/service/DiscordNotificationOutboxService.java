package br.com.ricardo.vagaradar.service;

import br.com.ricardo.vagaradar.config.DiscordProperties;
import br.com.ricardo.vagaradar.entity.AnaliseVaga;
import br.com.ricardo.vagaradar.entity.NotificacaoPendente;
import br.com.ricardo.vagaradar.entity.StatusNotificacao;
import br.com.ricardo.vagaradar.integration.discord.DiscordNotifier;
import br.com.ricardo.vagaradar.repository.NotificacaoPendenteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
public class DiscordNotificationOutboxService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DiscordNotificationOutboxService.class);
    private static final int MAX_TENTATIVAS = 5;

    private final NotificacaoPendenteRepository notificacaoRepository;
    private final DiscordNotifier discordNotifier;
    private final DiscordProperties discordProperties;

    public DiscordNotificationOutboxService(
            NotificacaoPendenteRepository notificacaoRepository,
            DiscordNotifier discordNotifier,
            DiscordProperties discordProperties
    ) {
        this.notificacaoRepository = notificacaoRepository;
        this.discordNotifier = discordNotifier;
        this.discordProperties = discordProperties;
    }

    @Transactional
    public void registrarSeElegivel(AnaliseVaga analise) {
        boolean discordConfigurado = discordProperties.webhookUrl() != null && !discordProperties.webhookUrl().isBlank();
        if (discordConfigurado && analise.getPontuacao() >= discordProperties.minimumScore()) {
            notificacaoRepository.save(new NotificacaoPendente(analise));
        }
    }

    public void processarPendentes() {
        while (processarProxima()) {
            // Processa uma notificação por transação para isolar falhas e liberar o bloqueio da linha.
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean processarProxima() {
        List<NotificacaoPendente> pendentes = notificacaoRepository.buscarPendentesParaEnvio(
                StatusNotificacao.PENDENTE, Instant.now(), PageRequest.of(0, 1)
        );
        if (pendentes.isEmpty()) {
            return false;
        }

        NotificacaoPendente notificacao = pendentes.getFirst();
        try {
            discordNotifier.notificarAnalise(notificacao.getAnalise().getVaga(), notificacao.getAnalise());
            notificacao.marcarComoEnviada();
        } catch (RuntimeException exception) {
            boolean esgotouTentativas = notificacao.getTentativas() + 1 >= MAX_TENTATIVAS;
            notificacao.registrarFalha(resumirErro(exception), proximaTentativa(notificacao.getTentativas() + 1), esgotouTentativas);
            LOGGER.warn("Falha ao enviar alerta Discord da análise {} (tentativa {}/{}).",
                    notificacao.getAnalise().getId(), notificacao.getTentativas(), MAX_TENTATIVAS, exception);
        }
        return true;
    }

    private Instant proximaTentativa(int tentativa) {
        long segundos = Math.min(30L * (1L << Math.min(tentativa - 1, 6)), Duration.ofHours(1).toSeconds());
        return Instant.now().plusSeconds(segundos);
    }

    private String resumirErro(RuntimeException exception) {
        String mensagem = exception.getMessage();
        return mensagem == null || mensagem.isBlank() ? exception.getClass().getSimpleName() : mensagem;
    }
}

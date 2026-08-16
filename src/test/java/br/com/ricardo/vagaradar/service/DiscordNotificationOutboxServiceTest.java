package br.com.ricardo.vagaradar.service;

import br.com.ricardo.vagaradar.config.DiscordProperties;
import br.com.ricardo.vagaradar.entity.AnaliseVaga;
import br.com.ricardo.vagaradar.entity.NivelCompatibilidade;
import br.com.ricardo.vagaradar.entity.NotificacaoPendente;
import br.com.ricardo.vagaradar.entity.StatusNotificacao;
import br.com.ricardo.vagaradar.entity.Vaga;
import br.com.ricardo.vagaradar.integration.discord.DiscordNotifier;
import br.com.ricardo.vagaradar.repository.NotificacaoPendenteRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DiscordNotificationOutboxServiceTest {

    @Mock
    private NotificacaoPendenteRepository notificacaoRepository;

    @Mock
    private DiscordNotifier discordNotifier;

    @Test
    void deveRegistrarSomenteAnalisesElegiveis() {
        DiscordNotificationOutboxService service = serviceComMinimo(70);
        AnaliseVaga analise = analiseComPontuacao(80);

        service.registrarSeElegivel(analise);

        ArgumentCaptor<NotificacaoPendente> captor = ArgumentCaptor.forClass(NotificacaoPendente.class);
        verify(notificacaoRepository).save(captor.capture());
        assertThat(captor.getValue().getAnalise()).isSameAs(analise);
        assertThat(captor.getValue().getStatus()).isEqualTo(StatusNotificacao.PENDENTE);
    }

    @Test
    void naoDeveRegistrarAnaliseAbaixoDaNotaMinima() {
        DiscordNotificationOutboxService service = serviceComMinimo(70);

        service.registrarSeElegivel(analiseComPontuacao(69));

        verify(notificacaoRepository, never()).save(any());
    }

    @Test
    void deveMarcarComoEnviadaSemChamarOpenAiNovamente() {
        DiscordNotificationOutboxService service = serviceComMinimo(70);
        NotificacaoPendente pendente = new NotificacaoPendente(analiseComPontuacao(80));
        when(notificacaoRepository.buscarPendentesParaEnvio(eq(StatusNotificacao.PENDENTE), any(Instant.class), any(Pageable.class)))
                .thenReturn(List.of(pendente));

        boolean processada = service.processarProxima();

        assertThat(processada).isTrue();
        assertThat(pendente.getStatus()).isEqualTo(StatusNotificacao.ENVIADA);
        verify(discordNotifier).notificarAnalise(any(Vaga.class), eq(pendente.getAnalise()));
    }

    @Test
    void deveReagendarQuandoDiscordFalhar() {
        DiscordNotificationOutboxService service = serviceComMinimo(70);
        NotificacaoPendente pendente = new NotificacaoPendente(analiseComPontuacao(80));
        Instant antesDoEnvio = Instant.now();
        given(notificacaoRepository.buscarPendentesParaEnvio(eq(StatusNotificacao.PENDENTE), any(Instant.class), any(Pageable.class)))
                .willReturn(List.of(pendente));
        org.mockito.Mockito.doThrow(new IllegalStateException("Discord indisponível"))
                .when(discordNotifier).notificarAnalise(any(Vaga.class), any(AnaliseVaga.class));

        service.processarProxima();

        assertThat(pendente.getStatus()).isEqualTo(StatusNotificacao.PENDENTE);
        assertThat(pendente.getTentativas()).isEqualTo(1);
        assertThat(pendente.getProximaTentativaEm()).isAfter(antesDoEnvio);
        assertThat(pendente.getUltimoErro()).isEqualTo("Discord indisponível");
    }

    private DiscordNotificationOutboxService serviceComMinimo(int minimo) {
        return new DiscordNotificationOutboxService(
                notificacaoRepository, discordNotifier, new DiscordProperties("https://discord.example/webhook", minimo)
        );
    }

    private AnaliseVaga analiseComPontuacao(int pontuacao) {
        Vaga vaga = new Vaga("123", "Desenvolvedor Java", "Empresa", "Java e Spring", "São Paulo",
                br.com.ricardo.vagaradar.entity.ModeloTrabalho.HIBRIDO, "https://example.com/vaga", null);
        return new AnaliseVaga(vaga, pontuacao, NivelCompatibilidade.ALTA, "Java", "Cloud", "Candidatar");
    }
}

package br.com.ricardo.vagaradar.service;

import br.com.ricardo.vagaradar.dto.PerfilProfissionalRequest;
import br.com.ricardo.vagaradar.dto.PerfilProfissionalResponse;
import br.com.ricardo.vagaradar.entity.PerfilProfissional;
import br.com.ricardo.vagaradar.repository.PerfilProfissionalRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PerfilProfissionalServiceTest {

    private final PerfilProfissionalRepository repository = mock(PerfilProfissionalRepository.class);
    private final PerfilProfissionalService service = new PerfilProfissionalService(repository);

    @Test
    void deveRetornarPerfilPadraoQuandoNaoHaPersonalizacao() {
        when(repository.findById(PerfilProfissional.ID_UNICO)).thenReturn(Optional.empty());

        PerfilProfissionalResponse perfil = service.obter();

        assertThat(perfil.personalizado()).isFalse();
        assertThat(perfil.stackPrincipal()).contains("Java", "Spring Boot");
        assertThat(service.obterParaAnalise()).contains("Objetivo:", "Preferência:");
    }

    @Test
    void deveSalvarUmaPersonalizacaoComoSobreposicaoDoPadrao() {
        when(repository.findById(PerfilProfissional.ID_UNICO)).thenReturn(Optional.empty());
        when(repository.save(org.mockito.ArgumentMatchers.any(PerfilProfissional.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        PerfilProfissionalRequest request = new PerfilProfissionalRequest(
                "Backend júnior", "Java e Spring", "Docker", "Engenharia de Software", "Projetos", "Remoto"
        );

        PerfilProfissionalResponse perfil = service.atualizar(request);

        ArgumentCaptor<PerfilProfissional> captor = ArgumentCaptor.forClass(PerfilProfissional.class);
        verify(repository).save(captor.capture());
        assertThat(perfil.personalizado()).isTrue();
        assertThat(captor.getValue().getObjetivo()).isEqualTo("Backend júnior");
    }

    @Test
    void deveRemoverApenasASobreposicaoAoRestaurarPadrao() {
        service.restaurarPadrao();

        verify(repository).deleteById(PerfilProfissional.ID_UNICO);
    }
}

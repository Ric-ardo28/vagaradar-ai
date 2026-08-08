package br.com.ricardo.vagaradar.service;

import br.com.ricardo.vagaradar.dto.PerfilProfissionalRequest;
import br.com.ricardo.vagaradar.dto.PerfilProfissionalResponse;
import br.com.ricardo.vagaradar.entity.PerfilProfissional;
import br.com.ricardo.vagaradar.repository.PerfilProfissionalRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PerfilProfissionalService {

    private static final PerfilProfissionalResponse PERFIL_PADRAO = new PerfilProfissionalResponse(
            "Estágio ou Desenvolvedor Backend Júnior.",
            "Java, Spring Boot, APIs REST, JPA/Hibernate, PostgreSQL, SQL e Git.",
            "Docker, HTML, CSS e JavaScript.",
            "Engenharia de Software em andamento.",
            "Projetos pessoais e acadêmicos.",
            "São Paulo, híbrido ou remoto.",
            false,
            null
    );

    private final PerfilProfissionalRepository repository;

    public PerfilProfissionalService(PerfilProfissionalRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public PerfilProfissionalResponse obter() {
        return repository.findById(PerfilProfissional.ID_UNICO)
                .map(this::paraResposta)
                .orElse(PERFIL_PADRAO);
    }

    @Transactional
    public PerfilProfissionalResponse atualizar(PerfilProfissionalRequest request) {
        PerfilProfissional perfil = repository.findById(PerfilProfissional.ID_UNICO)
                .orElseGet(() -> new PerfilProfissional(
                        request.objetivo(), request.stackPrincipal(), request.conhecimentosBasicos(),
                        request.formacao(), request.experiencia(), request.preferencias()
                ));

        perfil.atualizar(
                request.objetivo(), request.stackPrincipal(), request.conhecimentosBasicos(),
                request.formacao(), request.experiencia(), request.preferencias()
        );
        return paraResposta(repository.save(perfil));
    }

    @Transactional
    public void restaurarPadrao() {
        repository.deleteById(PerfilProfissional.ID_UNICO);
    }

    @Transactional(readOnly = true)
    public String obterParaAnalise() {
        PerfilProfissionalResponse perfil = obter();
        return """
                Objetivo: %s
                Stack principal: %s
                Conhecimento básico: %s
                Formação: %s
                Experiência: %s
                Preferência: %s
                """.formatted(
                perfil.objetivo(), perfil.stackPrincipal(), perfil.conhecimentosBasicos(),
                perfil.formacao(), perfil.experiencia(), perfil.preferencias()
        );
    }

    private PerfilProfissionalResponse paraResposta(PerfilProfissional perfil) {
        return new PerfilProfissionalResponse(
                perfil.getObjetivo(), perfil.getStackPrincipal(), perfil.getConhecimentosBasicos(),
                perfil.getFormacao(), perfil.getExperiencia(), perfil.getPreferencias(), true, perfil.getAtualizadoEm()
        );
    }
}

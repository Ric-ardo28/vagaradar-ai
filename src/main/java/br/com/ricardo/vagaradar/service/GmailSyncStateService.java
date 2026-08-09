package br.com.ricardo.vagaradar.service;

import br.com.ricardo.vagaradar.entity.GmailSyncState;
import br.com.ricardo.vagaradar.repository.GmailSyncStateRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Service
@ConditionalOnProperty(prefix = "gmail", name = "oauth-enabled", havingValue = "true")
public class GmailSyncStateService {

    private final GmailSyncStateRepository repository;

    public GmailSyncStateService(GmailSyncStateRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public Optional<Instant> ultimoSucesso() {
        return repository.findById(GmailSyncState.ID_UNICO)
                .map(GmailSyncState::getLastSuccessfulSyncAt);
    }

    @Transactional
    public void registrarSucesso(Instant instante) {
        GmailSyncState state = repository.findById(GmailSyncState.ID_UNICO)
                .orElseGet(() -> new GmailSyncState(instante));
        state.atualizar(instante);
        repository.save(state);
    }
}

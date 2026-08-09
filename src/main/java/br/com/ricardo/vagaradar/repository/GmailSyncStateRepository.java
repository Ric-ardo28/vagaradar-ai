package br.com.ricardo.vagaradar.repository;

import br.com.ricardo.vagaradar.entity.GmailSyncState;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GmailSyncStateRepository extends JpaRepository<GmailSyncState, Short> {
}

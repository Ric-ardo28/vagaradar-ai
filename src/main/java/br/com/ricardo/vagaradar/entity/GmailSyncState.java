package br.com.ricardo.vagaradar.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Getter
@Entity
@Table(name = "gmail_sync_state")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GmailSyncState {

    public static final short ID_UNICO = 1;

    @Id
    private short id = ID_UNICO;

    @Column(name = "last_successful_sync_at", nullable = false)
    private Instant lastSuccessfulSyncAt;

    public GmailSyncState(Instant lastSuccessfulSyncAt) {
        this.lastSuccessfulSyncAt = lastSuccessfulSyncAt;
    }

    public void atualizar(Instant lastSuccessfulSyncAt) {
        this.lastSuccessfulSyncAt = lastSuccessfulSyncAt;
    }
}

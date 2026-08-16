package br.com.ricardo.vagaradar.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Getter
@Entity
@Table(name = "notificacao_pendente")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificacaoPendente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "analise_id", nullable = false, unique = true)
    private AnaliseVaga analise;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusNotificacao status = StatusNotificacao.PENDENTE;

    @Column(nullable = false)
    private int tentativas;

    @Column(name = "proxima_tentativa_em", nullable = false)
    private Instant proximaTentativaEm;

    @Column(name = "ultimo_erro", columnDefinition = "TEXT")
    private String ultimoErro;

    @Column(name = "criada_em", nullable = false, updatable = false)
    private Instant criadaEm;

    @Column(name = "enviada_em")
    private Instant enviadaEm;

    public NotificacaoPendente(AnaliseVaga analise) {
        this.analise = analise;
        this.proximaTentativaEm = Instant.now();
    }

    @PrePersist
    void preencherDatas() {
        if (criadaEm == null) {
            criadaEm = Instant.now();
        }
        if (proximaTentativaEm == null) {
            proximaTentativaEm = criadaEm;
        }
    }

    public void marcarComoEnviada() {
        status = StatusNotificacao.ENVIADA;
        enviadaEm = Instant.now();
        ultimoErro = null;
    }

    public void registrarFalha(String erro, Instant proximaTentativa, boolean esgotouTentativas) {
        tentativas++;
        ultimoErro = erro;
        proximaTentativaEm = proximaTentativa;
        status = esgotouTentativas ? StatusNotificacao.FALHOU : StatusNotificacao.PENDENTE;
    }
}

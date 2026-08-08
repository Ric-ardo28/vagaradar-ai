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
import jakarta.persistence.Table;
import jakarta.persistence.PrePersist;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Getter
@Entity
@Table(name = "analise_vaga")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AnaliseVaga {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vaga_id", nullable = false, unique = true)
    private Vaga vaga;

    @Column(nullable = false)
    private Integer pontuacao;

    @Enumerated(EnumType.STRING)
    @Column(name = "nivel_compatibilidade", nullable = false, length = 20)
    private NivelCompatibilidade nivelCompatibilidade;

    @Column(name = "pontos_fortes", columnDefinition = "TEXT")
    private String pontosFortes;

    @Column(name = "pontos_faltantes", columnDefinition = "TEXT")
    private String pontosFaltantes;

    @Column(columnDefinition = "TEXT")
    private String recomendacao;

    @Column(name = "analisada_em", nullable = false, updatable = false)
    private Instant analisadaEm;

    public AnaliseVaga(
            Vaga vaga,
            Integer pontuacao,
            NivelCompatibilidade nivelCompatibilidade,
            String pontosFortes,
            String pontosFaltantes,
            String recomendacao
    ) {
        this.vaga = vaga;
        this.pontuacao = pontuacao;
        this.nivelCompatibilidade = nivelCompatibilidade;
        this.pontosFortes = pontosFortes;
        this.pontosFaltantes = pontosFaltantes;
        this.recomendacao = recomendacao;
    }

    @PrePersist
    void preencherDataAnalise() {
        if (analisadaEm == null) {
            analisadaEm = Instant.now();
        }
    }
}

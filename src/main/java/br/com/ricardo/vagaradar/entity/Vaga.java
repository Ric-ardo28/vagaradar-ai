package br.com.ricardo.vagaradar.entity;

import jakarta.persistence.Column;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

@Getter
@Entity
@Table(name = "vaga")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Vaga {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "linkedin_id", unique = true, length = 100)
    private String linkedinId;

    @Column(nullable = false, length = 255)
    private String cargo;

    @Column(nullable = false, length = 255)
    private String empresa;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String descricao;

    @Column(length = 255)
    private String localizacao;

    @Enumerated(EnumType.STRING)
    @Column(name = "modelo_trabalho", nullable = false, length = 30)
    private ModeloTrabalho modeloTrabalho = ModeloTrabalho.NAO_INFORMADO;

    @Column(nullable = false, length = 2048)
    private String link;

    @Column(name = "data_publicacao")
    private Instant dataPublicacao;

    @Column(name = "data_encontrada", nullable = false, updatable = false)
    private Instant dataEncontrada;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatusVaga status = StatusVaga.RECEBIDA;

    @Enumerated(EnumType.STRING)
    @Column(name = "avaliacao_usuario", nullable = false, length = 30)
    private AvaliacaoUsuario avaliacaoUsuario = AvaliacaoUsuario.PENDENTE;

    @ElementCollection
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "vaga_motivo_rejeicao", joinColumns = @JoinColumn(name = "vaga_id"))
    @Column(name = "motivo", nullable = false, length = 40)
    private Set<MotivoRejeicao> motivosRejeicao = new LinkedHashSet<>();

    @Column(name = "outro_motivo_rejeicao", columnDefinition = "TEXT")
    private String outroMotivoRejeicao;

    public Vaga(
            String linkedinId,
            String cargo,
            String empresa,
            String descricao,
            String localizacao,
            ModeloTrabalho modeloTrabalho,
            String link,
            Instant dataPublicacao
    ) {
        this.linkedinId = linkedinId;
        this.cargo = cargo;
        this.empresa = empresa;
        this.descricao = descricao;
        this.localizacao = localizacao;
        this.modeloTrabalho = modeloTrabalho == null ? ModeloTrabalho.NAO_INFORMADO : modeloTrabalho;
        this.link = link;
        this.dataPublicacao = dataPublicacao;
    }

    @PrePersist
    void preencherDataEncontrada() {
        if (dataEncontrada == null) {
            dataEncontrada = Instant.now();
        }
    }

    public void marcarComoAnalisada() {
        this.status = StatusVaga.ANALISADA;
    }

    public void descartar() {
        this.status = StatusVaga.DESCARTADA;
    }

    public void avaliar(AvaliacaoUsuario avaliacao, Set<MotivoRejeicao> motivos, String outroMotivo) {
        this.avaliacaoUsuario = avaliacao;
        this.motivosRejeicao.clear();
        this.outroMotivoRejeicao = null;
        if (avaliacao == AvaliacaoUsuario.NAO_GOSTEI) {
            if (motivos != null) {
                this.motivosRejeicao.addAll(motivos);
            }
            if (this.motivosRejeicao.contains(MotivoRejeicao.OUTRO) && outroMotivo != null && !outroMotivo.isBlank()) {
                this.outroMotivoRejeicao = outroMotivo.trim();
            }
        }
    }
}

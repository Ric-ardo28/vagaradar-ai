package br.com.ricardo.vagaradar.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Getter
@Entity
@Table(name = "perfil_profissional")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PerfilProfissional {

    public static final short ID_UNICO = 1;

    @Id
    private short id = ID_UNICO;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String objetivo;

    @Column(name = "stack_principal", nullable = false, columnDefinition = "TEXT")
    private String stackPrincipal;

    @Column(name = "conhecimentos_basicos", nullable = false, columnDefinition = "TEXT")
    private String conhecimentosBasicos;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String formacao;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String experiencia;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String preferencias;

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    public PerfilProfissional(String objetivo, String stackPrincipal, String conhecimentosBasicos,
                              String formacao, String experiencia, String preferencias) {
        atualizar(objetivo, stackPrincipal, conhecimentosBasicos, formacao, experiencia, preferencias);
        atualizarData();
    }

    public void atualizar(String objetivo, String stackPrincipal, String conhecimentosBasicos,
                          String formacao, String experiencia, String preferencias) {
        this.objetivo = objetivo;
        this.stackPrincipal = stackPrincipal;
        this.conhecimentosBasicos = conhecimentosBasicos;
        this.formacao = formacao;
        this.experiencia = experiencia;
        this.preferencias = preferencias;
        atualizarData();
    }

    @PrePersist
    @PreUpdate
    void atualizarData() {
        atualizadoEm = Instant.now();
    }
}

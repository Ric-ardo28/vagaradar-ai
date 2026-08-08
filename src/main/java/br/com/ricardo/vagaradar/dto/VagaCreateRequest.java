package br.com.ricardo.vagaradar.dto;

import br.com.ricardo.vagaradar.entity.ModeloTrabalho;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

import java.time.Instant;

public record VagaCreateRequest(
        @Size(max = 100) String linkedinId,
        @NotBlank @Size(max = 255) String cargo,
        @NotBlank @Size(max = 255) String empresa,
        @NotBlank String descricao,
        @Size(max = 255) String localizacao,
        ModeloTrabalho modeloTrabalho,
        @NotBlank @Size(max = 2048) @URL String link,
        Instant dataPublicacao
) {
}

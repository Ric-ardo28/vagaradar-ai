ALTER TABLE vaga
    ADD COLUMN avaliacao_usuario VARCHAR(30) NOT NULL DEFAULT 'PENDENTE',
    ADD COLUMN outro_motivo_rejeicao TEXT;

CREATE TABLE vaga_motivo_rejeicao (
    vaga_id BIGINT NOT NULL REFERENCES vaga(id) ON DELETE CASCADE,
    motivo VARCHAR(40) NOT NULL,
    PRIMARY KEY (vaga_id, motivo)
);

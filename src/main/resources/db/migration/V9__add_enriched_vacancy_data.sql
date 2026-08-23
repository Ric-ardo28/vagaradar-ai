ALTER TABLE vaga
    ADD COLUMN senioridade VARCHAR(255),
    ADD COLUMN requisitos_principais TEXT;

CREATE TABLE vaga_tecnologia (
    vaga_id BIGINT NOT NULL REFERENCES vaga(id) ON DELETE CASCADE,
    tecnologia VARCHAR(100) NOT NULL,
    PRIMARY KEY (vaga_id, tecnologia)
);

CREATE TABLE vaga_habilidade (
    vaga_id BIGINT NOT NULL REFERENCES vaga(id) ON DELETE CASCADE,
    habilidade VARCHAR(255) NOT NULL,
    PRIMARY KEY (vaga_id, habilidade)
);

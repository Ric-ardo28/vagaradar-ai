CREATE TABLE vaga (
    id BIGSERIAL PRIMARY KEY,
    linkedin_id VARCHAR(100) UNIQUE,
    cargo VARCHAR(255) NOT NULL,
    empresa VARCHAR(255) NOT NULL,
    descricao TEXT NOT NULL,
    localizacao VARCHAR(255),
    modelo_trabalho VARCHAR(30) NOT NULL DEFAULT 'NAO_INFORMADO',
    link VARCHAR(2048) NOT NULL,
    data_publicacao TIMESTAMP WITH TIME ZONE,
    data_encontrada TIMESTAMP WITH TIME ZONE NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'RECEBIDA'
);

CREATE TABLE analise_vaga (
    id BIGSERIAL PRIMARY KEY,
    vaga_id BIGINT NOT NULL UNIQUE REFERENCES vaga(id),
    pontuacao INTEGER NOT NULL CHECK (pontuacao BETWEEN 0 AND 100),
    nivel_compatibilidade VARCHAR(20) NOT NULL,
    pontos_fortes TEXT,
    pontos_faltantes TEXT,
    recomendacao TEXT,
    analisada_em TIMESTAMP WITH TIME ZONE NOT NULL
);

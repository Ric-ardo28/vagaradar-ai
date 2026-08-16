CREATE TABLE notificacao_pendente (
    id BIGSERIAL PRIMARY KEY,
    analise_id BIGINT NOT NULL UNIQUE REFERENCES analise_vaga(id),
    status VARCHAR(20) NOT NULL,
    tentativas INTEGER NOT NULL DEFAULT 0,
    proxima_tentativa_em TIMESTAMP WITH TIME ZONE NOT NULL,
    ultimo_erro TEXT,
    criada_em TIMESTAMP WITH TIME ZONE NOT NULL,
    enviada_em TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_notificacao_pendente_envio
    ON notificacao_pendente (status, proxima_tentativa_em, criada_em);

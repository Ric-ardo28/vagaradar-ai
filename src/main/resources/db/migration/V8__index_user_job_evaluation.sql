CREATE INDEX idx_vaga_avaliacao_encontrada
    ON vaga (avaliacao_usuario, data_encontrada DESC, id DESC);

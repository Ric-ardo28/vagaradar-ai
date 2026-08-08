CREATE TABLE perfil_profissional (
    id SMALLINT PRIMARY KEY CHECK (id = 1),
    objetivo TEXT NOT NULL,
    stack_principal TEXT NOT NULL,
    conhecimentos_basicos TEXT NOT NULL,
    formacao TEXT NOT NULL,
    experiencia TEXT NOT NULL,
    preferencias TEXT NOT NULL,
    atualizado_em TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE interacoes_assistente_gestor (
    id UUID PRIMARY KEY,
    pergunta TEXT NOT NULL,
    resposta TEXT NOT NULL,
    ferramentas_chamadas TEXT,
    criado_em TIMESTAMPTZ NOT NULL
);

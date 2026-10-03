-- Funcionário conversa com o assistente do gestor pelo próprio WhatsApp, após verificar o número.
ALTER TABLE usuarios ADD COLUMN telefone_whatsapp VARCHAR(20) UNIQUE;

CREATE TABLE verificacoes_whatsapp (
    usuario_id UUID PRIMARY KEY REFERENCES usuarios(id) ON DELETE CASCADE,
    telefone VARCHAR(20) NOT NULL,
    codigo_hash VARCHAR(64) NOT NULL,
    criada_em TIMESTAMPTZ NOT NULL,
    tentativas_restantes INTEGER NOT NULL
);

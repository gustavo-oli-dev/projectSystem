CREATE TABLE conversas (
    id UUID PRIMARY KEY,
    telefone_whatsapp VARCHAR(20) NOT NULL,
    cliente_id UUID REFERENCES clientes(id),
    status VARCHAR(20) NOT NULL,
    atendente_id UUID REFERENCES usuarios(id),
    criada_em TIMESTAMPTZ NOT NULL,
    atualizada_em TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_conversas_telefone_status ON conversas (telefone_whatsapp, status);

CREATE TABLE mensagens (
    id UUID PRIMARY KEY,
    conversa_id UUID NOT NULL REFERENCES conversas(id),
    origem VARCHAR(10) NOT NULL,
    conteudo TEXT,
    id_externo_whatsapp VARCHAR(100),
    enviada_em TIMESTAMPTZ NOT NULL
);

CREATE TABLE anexos (
    id UUID PRIMARY KEY,
    mensagem_id UUID NOT NULL REFERENCES mensagens(id),
    mime_type VARCHAR(100) NOT NULL,
    tamanho_bytes BIGINT NOT NULL,
    checksum_sha256 VARCHAR(64) NOT NULL,
    chave_objeto VARCHAR(200) NOT NULL,
    nome_arquivo_original VARCHAR(255)
);

CREATE TABLE eventos_webhook_whatsapp (
    id UUID PRIMARY KEY,
    payload_bruto TEXT NOT NULL,
    status VARCHAR(20) NOT NULL,
    recebido_em TIMESTAMPTZ NOT NULL,
    processado_em TIMESTAMPTZ
);

CREATE TABLE usuarios (
    id UUID PRIMARY KEY,
    nome VARCHAR(150) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    senha_criptografada VARCHAR(100) NOT NULL,
    perfil VARCHAR(20) NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE clientes (
    id UUID PRIMARY KEY,
    nome VARCHAR(200) NOT NULL,
    documento VARCHAR(30) NOT NULL UNIQUE,
    telefone_whatsapp VARCHAR(20) NOT NULL,
    criado_em TIMESTAMPTZ NOT NULL DEFAULT now()
);

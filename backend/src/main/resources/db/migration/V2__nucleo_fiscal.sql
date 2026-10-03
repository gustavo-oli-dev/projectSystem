CREATE TABLE produtos (
    id UUID PRIMARY KEY,
    nome VARCHAR(200) NOT NULL,
    descricao VARCHAR(500),
    ncm VARCHAR(8) NOT NULL,
    unidade_medida VARCHAR(10) NOT NULL,
    preco_unitario NUMERIC(14,2) NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE servicos (
    id UUID PRIMARY KEY,
    nome VARCHAR(200) NOT NULL,
    descricao VARCHAR(500),
    codigo_servico_lc116 VARCHAR(5) NOT NULL,
    aliquota_iss NUMERIC(5,2) NOT NULL,
    preco_unitario NUMERIC(14,2) NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE pedidos (
    id UUID PRIMARY KEY,
    cliente_id UUID NOT NULL REFERENCES clientes(id),
    status VARCHAR(30) NOT NULL,
    criado_em TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE itens_pedido (
    pedido_id UUID NOT NULL REFERENCES pedidos(id) ON DELETE CASCADE,
    tipo VARCHAR(10) NOT NULL,
    referencia_id UUID NOT NULL,
    descricao VARCHAR(300) NOT NULL,
    preco_unitario NUMERIC(14,2) NOT NULL,
    quantidade INTEGER NOT NULL
);

CREATE TABLE documentos_fiscais (
    id UUID PRIMARY KEY,
    pedido_id UUID NOT NULL REFERENCES pedidos(id),
    tipo VARCHAR(10) NOT NULL,
    status VARCHAR(20) NOT NULL,
    xml_autorizado TEXT,
    protocolo VARCHAR(60),
    motivo_rejeicao VARCHAR(300),
    criado_em TIMESTAMPTZ NOT NULL,
    atualizado_em TIMESTAMPTZ NOT NULL
);

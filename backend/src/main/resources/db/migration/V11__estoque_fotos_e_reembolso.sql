-- Estoque, código de barras e fotos de produto; reembolso de cobrança (D18).

ALTER TABLE produtos ADD COLUMN quantidade_em_estoque INTEGER NOT NULL DEFAULT 0;
ALTER TABLE produtos ADD CONSTRAINT produtos_estoque_nao_negativo CHECK (quantidade_em_estoque >= 0);
ALTER TABLE produtos ALTER COLUMN quantidade_em_estoque DROP DEFAULT;

-- EAN/GTIN (campo cEAN da NF-e); chave do futuro app de escaneamento.
ALTER TABLE produtos ADD COLUMN codigo_barras VARCHAR(14) UNIQUE;

CREATE TABLE movimentacoes_estoque (
    id UUID PRIMARY KEY,
    produto_id UUID NOT NULL REFERENCES produtos(id),
    tipo VARCHAR(20) NOT NULL,
    quantidade INTEGER NOT NULL CHECK (quantidade > 0),
    saldo_apos INTEGER NOT NULL CHECK (saldo_apos >= 0),
    pedido_id UUID REFERENCES pedidos(id),
    responsavel VARCHAR(255) NOT NULL,
    criada_em TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_movimentacoes_estoque_produto ON movimentacoes_estoque (produto_id, criada_em DESC);

CREATE TABLE fotos_produto (
    id UUID PRIMARY KEY,
    produto_id UUID NOT NULL REFERENCES produtos(id) ON DELETE CASCADE,
    conteudo BYTEA NOT NULL,
    tipo VARCHAR(10) NOT NULL,
    tamanho_bytes INTEGER NOT NULL CHECK (tamanho_bytes > 0 AND tamanho_bytes <= 5242880),
    ordem INTEGER NOT NULL,
    criada_em TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_fotos_produto_produto ON fotos_produto (produto_id, ordem);

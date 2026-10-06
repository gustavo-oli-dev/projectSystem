-- Embalagens (A3 → D41): vender a unidade ou a embalagem (ex.: fardo com 12). O estoque continua em
-- unidades; a embalagem tem nome, preço próprio e, se tiver, código de barras próprio.
CREATE TABLE embalagens_produto (
    id UUID PRIMARY KEY,
    produto_id UUID NOT NULL REFERENCES produtos(id),
    nome VARCHAR(60) NOT NULL,
    codigo_barras VARCHAR(14) UNIQUE,
    unidades INTEGER NOT NULL CHECK (unidades >= 2 AND unidades <= 1000),
    preco NUMERIC(14, 2) NOT NULL CHECK (preco > 0),
    ativa BOOLEAN NOT NULL DEFAULT TRUE,
    criada_em TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_embalagens_produto ON embalagens_produto (produto_id);

-- Item vendido como embalagem: quantas unidades do produto cada uma tem (1 = vendido avulso).
ALTER TABLE itens_pedido ADD COLUMN unidades_por_embalagem INTEGER NOT NULL DEFAULT 1 CHECK (unidades_por_embalagem >= 1);

-- Histórico de preços (D39): toda mudança de preço (uma a uma ou em lote) fica registrada — base para
-- reimprimir as etiquetas de gôndola do que mudou e para conferir quem mudou o quê.
CREATE TABLE alteracoes_preco (
    id UUID PRIMARY KEY,
    produto_id UUID NOT NULL REFERENCES produtos(id),
    preco_anterior NUMERIC(14, 2) NOT NULL CHECK (preco_anterior >= 0),
    preco_novo NUMERIC(14, 2) NOT NULL CHECK (preco_novo > 0),
    alterado_por VARCHAR(255) NOT NULL,
    alterado_em TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_alteracoes_preco_data ON alteracoes_preco (alterado_em);

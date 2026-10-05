-- Desconto e cancelamento de item no caixa com autorização do gerente (D35).

-- Desconto rateado por item (a NFC-e exige o desconto por item; os relatórios descontam dele).
ALTER TABLE itens_pedido ADD COLUMN desconto NUMERIC(14, 2) NOT NULL DEFAULT 0 CHECK (desconto >= 0);
ALTER TABLE pedidos ADD COLUMN desconto_autorizado_por VARCHAR(255);

-- Item tirado da venda depois de lido: trilha para conferência (ponto clássico de fraude).
CREATE TABLE itens_cancelados_caixa (
    id UUID PRIMARY KEY,
    sessao_caixa_id UUID NOT NULL REFERENCES sessoes_caixa(id),
    produto_id UUID NOT NULL REFERENCES produtos(id),
    descricao VARCHAR(200) NOT NULL,
    quantidade INTEGER NOT NULL CHECK (quantidade > 0),
    preco_unitario NUMERIC(14, 2) NOT NULL CHECK (preco_unitario >= 0),
    operador VARCHAR(255) NOT NULL,
    autorizado_por VARCHAR(255) NOT NULL,
    cancelado_em TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_itens_cancelados_caixa_data ON itens_cancelados_caixa (cancelado_em);

-- Quem gerencia os caixas também autoriza desconto e cancelamento.
INSERT INTO cargo_permissoes (cargo_id, permissao)
SELECT cargo_id, 'PDV_AUTORIZAR' FROM cargo_permissoes WHERE permissao = 'CAIXA_GERENCIAR';

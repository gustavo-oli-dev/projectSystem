-- Relatórios (D21): custo do produto (para o lucro), custo congelado no item vendido e a data
-- em que a venda foi confirmada (é a data que "vendas do dia" usa).

ALTER TABLE produtos ADD COLUMN custo_unitario NUMERIC(14, 2) CHECK (custo_unitario >= 0);
ALTER TABLE itens_pedido ADD COLUMN custo_unitario NUMERIC(14, 2) CHECK (custo_unitario >= 0);

ALTER TABLE pedidos ADD COLUMN confirmado_em TIMESTAMPTZ;
-- Pedidos já confirmados antes desta migration: a melhor aproximação é a data de criação.
UPDATE pedidos SET confirmado_em = criado_em WHERE status IN ('AGUARDANDO_EMISSAO', 'CONCLUIDO');
-- Vendas do balcão confirmam na hora; as canceladas também foram vendas (desfeitas depois).
UPDATE pedidos SET confirmado_em = criado_em WHERE status = 'CANCELADO' AND canal = 'BALCAO';

CREATE INDEX idx_pedidos_confirmado_em ON pedidos (confirmado_em) WHERE confirmado_em IS NOT NULL;

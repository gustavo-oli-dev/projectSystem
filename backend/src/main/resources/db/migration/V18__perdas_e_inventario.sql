-- Perdas e quebras + inventário (D32). Produto vencido/estragado simplesmente sai do estoque com
-- motivo (não há controle de validade/lote, decisão do usuário). Inventário acerta o estoque à contagem.

ALTER TABLE movimentacoes_estoque ADD COLUMN motivo VARCHAR(30);
ALTER TABLE movimentacoes_estoque ADD COLUMN observacao VARCHAR(200);
-- Custo do produto no momento da perda: o relatório mostra quanto se perdeu em dinheiro.
ALTER TABLE movimentacoes_estoque ADD COLUMN custo_unitario NUMERIC(14, 2) CHECK (custo_unitario >= 0);
ALTER TABLE movimentacoes_estoque ADD CONSTRAINT movimentacoes_estoque_perda_tem_motivo
    CHECK (tipo <> 'PERDA' OR motivo IS NOT NULL);
CREATE INDEX idx_movimentacoes_estoque_tipo_data ON movimentacoes_estoque (tipo, criada_em);

-- Gestão de caixa separada da venda (D27): abrir, repor troco, sangria e fechar passam a ser feitos
-- por quem tem CAIXA_GERENCIAR, em qualquer caixa. O caixa continua pertencendo ao operador que vende.

ALTER TABLE sessoes_caixa ADD COLUMN aberta_por VARCHAR(255);
UPDATE sessoes_caixa SET aberta_por = operador;
ALTER TABLE sessoes_caixa ALTER COLUMN aberta_por SET NOT NULL;

ALTER TABLE sessoes_caixa ADD COLUMN fechada_por VARCHAR(255);
UPDATE sessoes_caixa SET fechada_por = operador WHERE status = 'FECHADA';
ALTER TABLE sessoes_caixa ADD CONSTRAINT sessoes_caixa_fechada_por_quem CHECK (status = 'ABERTA' OR fechada_por IS NOT NULL);

-- Quem já confere os caixas passa também a poder operá-los.
INSERT INTO cargo_permissoes (cargo_id, permissao)
SELECT cargo_id, 'CAIXA_GERENCIAR' FROM cargo_permissoes WHERE permissao = 'CAIXA_CONFERIR';

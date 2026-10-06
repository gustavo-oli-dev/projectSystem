-- Área do Financeiro (D40): fluxo de caixa (entradas, saídas, saldo por dia) com exportação.
-- Quem já cuida das contas a pagar ou vê o faturamento passa a ver o Financeiro.
INSERT INTO cargo_permissoes (cargo_id, permissao)
SELECT DISTINCT cargo_id, 'FINANCEIRO_VER' FROM cargo_permissoes
WHERE permissao IN ('CONTAS_PAGAR_GERENCIAR', 'FATURAMENTO_VER')
ON CONFLICT DO NOTHING;

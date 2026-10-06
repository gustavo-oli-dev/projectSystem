-- Uma permissão por aba do Painel (D44): quem administra os cargos escolhe quais abas cada cargo vê.
-- Ninguém perde acesso: cada cargo ganha as abas que já via pelas permissões antigas.
INSERT INTO cargo_permissoes (cargo_id, permissao)
SELECT DISTINCT cargo_id, aba FROM cargo_permissoes
CROSS JOIN (VALUES ('PAINEL_VENDAS'), ('PAINEL_PRODUTOS'), ('PAINEL_HORARIOS')) AS abas(aba)
WHERE permissao = 'FATURAMENTO_VER'
ON CONFLICT DO NOTHING;

INSERT INTO cargo_permissoes (cargo_id, permissao)
SELECT DISTINCT cargo_id, aba FROM cargo_permissoes
CROSS JOIN (VALUES ('PAINEL_CAIXA'), ('PAINEL_DINHEIRO_DO_DIA')) AS abas(aba)
WHERE permissao IN ('FATURAMENTO_VER', 'CAIXA_CONFERIR')
ON CONFLICT DO NOTHING;

-- "Ver o painel" vira a aba "Operação agora" (as outras abas têm permissão própria agora).
INSERT INTO cargo_permissoes (cargo_id, permissao)
SELECT DISTINCT cargo_id, 'PAINEL_OPERACAO' FROM cargo_permissoes WHERE permissao = 'PAINEL_VER'
ON CONFLICT DO NOTHING;
DELETE FROM cargo_permissoes WHERE permissao = 'PAINEL_VER';

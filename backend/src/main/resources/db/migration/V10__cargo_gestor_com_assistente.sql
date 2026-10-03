-- Cargo pronto para quem acompanha a empresa pelo assistente: vê todas as áreas e conversa com o
-- assistente de IA, mas não cadastra, confirma, cobra nem emite nada. Pedido do usuário em 03/10.
INSERT INTO cargos (id, nome, descricao, criado_em)
SELECT gen_random_uuid(), 'Gestor com assistente',
       'Vê todas as áreas e conversa com o assistente de IA; não altera nada', now()
WHERE NOT EXISTS (SELECT 1 FROM cargos WHERE nome = 'Gestor com assistente');

INSERT INTO cargo_permissoes (cargo_id, permissao)
SELECT c.id, p.permissao
FROM cargos c
CROSS JOIN (VALUES
    ('ASSISTENTE_GESTOR_USAR'),
    ('PAINEL_VER'),
    ('FATURAMENTO_VER'),
    ('PEDIDOS_VER'),
    ('CLIENTES_VER'),
    ('CATALOGO_VER'),
    ('FISCAL_VER'),
    ('COBRANCAS_VER'),
    ('CONVERSAS_VER')
) AS p(permissao)
WHERE c.nome = 'Gestor com assistente'
ON CONFLICT DO NOTHING;

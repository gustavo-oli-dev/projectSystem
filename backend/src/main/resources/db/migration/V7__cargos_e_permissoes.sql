-- Hierarquia de acesso: o perfil fixo (ADMIN/FINANCEIRO/ATENDENTE) vira cargos configuráveis com
-- permissões do catálogo fixo (enum Permissao). Dono e programador têm acesso irrestrito.

CREATE TABLE cargos (
    id UUID PRIMARY KEY,
    nome VARCHAR(80) NOT NULL UNIQUE,
    descricao VARCHAR(255),
    criado_em TIMESTAMPTZ NOT NULL
);

CREATE TABLE cargo_permissoes (
    cargo_id UUID NOT NULL REFERENCES cargos(id) ON DELETE CASCADE,
    permissao VARCHAR(40) NOT NULL,
    PRIMARY KEY (cargo_id, permissao)
);

-- Cargos iniciais equivalentes aos perfis antigos, mais o exemplo de gerente sem faturamento/SEFAZ.
INSERT INTO cargos (id, nome, descricao, criado_em) VALUES
    (gen_random_uuid(), 'Atendimento (call center)', 'Atende conversas do WhatsApp e consulta clientes e pedidos', now()),
    (gen_random_uuid(), 'Financeiro', 'Faturamento, cobranças e consulta fiscal', now()),
    (gen_random_uuid(), 'Gerente comercial', 'Pedidos, clientes, catálogo e conversas; sem faturamento nem SEFAZ', now());

INSERT INTO cargo_permissoes (cargo_id, permissao)
SELECT c.id, p.permissao
FROM cargos c
JOIN (VALUES
    ('Atendimento (call center)', 'CONVERSAS_VER'),
    ('Atendimento (call center)', 'CONVERSAS_ATENDER'),
    ('Atendimento (call center)', 'CLIENTES_VER'),
    ('Atendimento (call center)', 'PEDIDOS_VER'),
    ('Atendimento (call center)', 'CATALOGO_VER'),
    ('Financeiro', 'PAINEL_VER'),
    ('Financeiro', 'FATURAMENTO_VER'),
    ('Financeiro', 'PEDIDOS_VER'),
    ('Financeiro', 'CLIENTES_VER'),
    ('Financeiro', 'COBRANCAS_VER'),
    ('Financeiro', 'COBRANCAS_GERENCIAR'),
    ('Financeiro', 'FISCAL_VER'),
    ('Gerente comercial', 'PAINEL_VER'),
    ('Gerente comercial', 'PEDIDOS_VER'),
    ('Gerente comercial', 'PEDIDOS_GERENCIAR'),
    ('Gerente comercial', 'CLIENTES_VER'),
    ('Gerente comercial', 'CLIENTES_GERENCIAR'),
    ('Gerente comercial', 'CATALOGO_VER'),
    ('Gerente comercial', 'CATALOGO_GERENCIAR'),
    ('Gerente comercial', 'CONVERSAS_VER'),
    ('Gerente comercial', 'CONVERSAS_ATENDER')
) AS p(nome_cargo, permissao) ON p.nome_cargo = c.nome;

ALTER TABLE usuarios ADD COLUMN acesso_irrestrito BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE usuarios ADD COLUMN cargo_id UUID REFERENCES cargos(id);

UPDATE usuarios SET acesso_irrestrito = TRUE WHERE perfil = 'ADMIN';
UPDATE usuarios SET cargo_id = (SELECT id FROM cargos WHERE nome = 'Financeiro') WHERE perfil = 'FINANCEIRO';
UPDATE usuarios SET cargo_id = (SELECT id FROM cargos WHERE nome = 'Atendimento (call center)') WHERE perfil = 'ATENDENTE';

ALTER TABLE usuarios DROP COLUMN perfil;
ALTER TABLE usuarios ADD CONSTRAINT usuarios_tem_acesso CHECK (acesso_irrestrito OR cargo_id IS NOT NULL);
CREATE INDEX idx_usuarios_cargo ON usuarios (cargo_id);

-- Auditoria do assistente: quem perguntou. Interações antigas vieram só do WhatsApp do dono.
ALTER TABLE interacoes_assistente_gestor ADD COLUMN solicitante VARCHAR(255);
UPDATE interacoes_assistente_gestor SET solicitante = 'WhatsApp do dono' WHERE solicitante IS NULL;
ALTER TABLE interacoes_assistente_gestor ALTER COLUMN solicitante SET NOT NULL;

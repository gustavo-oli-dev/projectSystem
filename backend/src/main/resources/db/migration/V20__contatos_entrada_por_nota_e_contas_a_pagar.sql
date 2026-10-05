-- Contatos, entrada de mercadoria pelo XML da nota do fornecedor e contas a pagar (D34).

CREATE TABLE contatos (
    id UUID PRIMARY KEY,
    tipo VARCHAR(20) NOT NULL,
    nome VARCHAR(150) NOT NULL,
    -- CNPJ/CPF normalizado (sem pontuação); único quando informado (o fornecedor da nota é achado por ele).
    documento VARCHAR(14) UNIQUE,
    telefone VARCHAR(30),
    email VARCHAR(150),
    observacao VARCHAR(500),
    ativo BOOLEAN NOT NULL,
    criado_em TIMESTAMPTZ NOT NULL
);

CREATE TABLE notas_entrada (
    id UUID PRIMARY KEY,
    -- A mesma nota nunca entra duas vezes no estoque.
    chave_acesso CHAR(44) NOT NULL UNIQUE,
    numero VARCHAR(9) NOT NULL,
    serie VARCHAR(3) NOT NULL,
    emitida_em TIMESTAMPTZ NOT NULL,
    fornecedor_id UUID NOT NULL REFERENCES contatos(id),
    valor_total NUMERIC(14, 2) NOT NULL CHECK (valor_total >= 0),
    registrada_por VARCHAR(255) NOT NULL,
    registrada_em TIMESTAMPTZ NOT NULL
);

CREATE TABLE itens_nota_entrada (
    nota_entrada_id UUID NOT NULL REFERENCES notas_entrada(id),
    ordem INTEGER NOT NULL,
    produto_id UUID NOT NULL REFERENCES produtos(id),
    descricao_na_nota VARCHAR(200) NOT NULL,
    quantidade INTEGER NOT NULL CHECK (quantidade > 0),
    custo_unitario NUMERIC(14, 4) NOT NULL CHECK (custo_unitario >= 0),
    PRIMARY KEY (nota_entrada_id, ordem)
);

ALTER TABLE movimentacoes_estoque ADD COLUMN nota_entrada_id UUID REFERENCES notas_entrada(id);

CREATE TABLE contas_a_pagar (
    id UUID PRIMARY KEY,
    contato_id UUID REFERENCES contatos(id),
    descricao VARCHAR(200) NOT NULL,
    valor NUMERIC(14, 2) NOT NULL CHECK (valor > 0),
    vencimento DATE NOT NULL,
    status VARCHAR(20) NOT NULL,
    nota_entrada_id UUID REFERENCES notas_entrada(id),
    criada_por VARCHAR(255) NOT NULL,
    criada_em TIMESTAMPTZ NOT NULL,
    paga_em TIMESTAMPTZ,
    paga_por VARCHAR(255),
    CONSTRAINT contas_a_pagar_paga_tem_quem_e_quando CHECK (status <> 'PAGA' OR (paga_em IS NOT NULL AND paga_por IS NOT NULL))
);
CREATE INDEX idx_contas_a_pagar_status_vencimento ON contas_a_pagar (status, vencimento);

INSERT INTO cargo_permissoes (cargo_id, permissao)
SELECT c.id, p.permissao FROM cargos c
JOIN (VALUES ('CONTATOS_GERENCIAR'), ('CONTAS_PAGAR_GERENCIAR')) AS p(permissao) ON TRUE
WHERE c.nome = 'Financeiro';

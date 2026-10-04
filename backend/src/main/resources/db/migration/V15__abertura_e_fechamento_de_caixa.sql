-- Abertura e fechamento de caixa (D26): fundo de troco contado por cédula, reposição de troco
-- (suprimento), sangria e conferência no fechamento. Toda venda do balcão pertence a um caixa aberto.

CREATE TABLE sessoes_caixa (
    id UUID PRIMARY KEY,
    operador VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL,
    aberta_em TIMESTAMPTZ NOT NULL,
    fundo_inicial NUMERIC(14, 2) NOT NULL CHECK (fundo_inicial >= 0),
    fechada_em TIMESTAMPTZ,
    valor_contado NUMERIC(14, 2) CHECK (valor_contado >= 0),
    vendas_em_dinheiro NUMERIC(14, 2) CHECK (vendas_em_dinheiro >= 0),
    valor_esperado NUMERIC(14, 2),
    observacao_fechamento VARCHAR(500),
    CONSTRAINT sessoes_caixa_fechamento_completo CHECK (
        status = 'ABERTA'
        OR (fechada_em IS NOT NULL AND valor_contado IS NOT NULL AND vendas_em_dinheiro IS NOT NULL AND valor_esperado IS NOT NULL)
    )
);
-- Um operador só pode ter um caixa aberto por vez (vale também para duas abas abrindo juntas).
CREATE UNIQUE INDEX uq_sessoes_caixa_uma_aberta_por_operador ON sessoes_caixa (operador) WHERE status = 'ABERTA';
CREATE INDEX idx_sessoes_caixa_aberta_em ON sessoes_caixa (aberta_em DESC);

CREATE TABLE sessoes_caixa_cedulas_abertura (
    sessao_caixa_id UUID NOT NULL REFERENCES sessoes_caixa(id),
    cedula VARCHAR(20) NOT NULL,
    quantidade INTEGER NOT NULL CHECK (quantidade > 0),
    PRIMARY KEY (sessao_caixa_id, cedula)
);

CREATE TABLE sessoes_caixa_cedulas_fechamento (
    sessao_caixa_id UUID NOT NULL REFERENCES sessoes_caixa(id),
    cedula VARCHAR(20) NOT NULL,
    quantidade INTEGER NOT NULL CHECK (quantidade > 0),
    PRIMARY KEY (sessao_caixa_id, cedula)
);

CREATE TABLE movimentos_caixa (
    id UUID PRIMARY KEY,
    sessao_caixa_id UUID NOT NULL REFERENCES sessoes_caixa(id),
    tipo VARCHAR(20) NOT NULL,
    valor NUMERIC(14, 2) NOT NULL CHECK (valor > 0),
    motivo VARCHAR(200) NOT NULL,
    registrado_por VARCHAR(255) NOT NULL,
    registrado_em TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_movimentos_caixa_sessao ON movimentos_caixa (sessao_caixa_id);

CREATE TABLE movimentos_caixa_cedulas (
    movimento_caixa_id UUID NOT NULL REFERENCES movimentos_caixa(id),
    cedula VARCHAR(20) NOT NULL,
    quantidade INTEGER NOT NULL CHECK (quantidade > 0),
    PRIMARY KEY (movimento_caixa_id, cedula)
);

-- Composição padrão do fundo de troco definida pelo gerente (vem preenchida na abertura).
CREATE TABLE fundo_troco_padrao (
    cedula VARCHAR(20) PRIMARY KEY,
    quantidade INTEGER NOT NULL CHECK (quantidade > 0)
);

ALTER TABLE pedidos ADD COLUMN sessao_caixa_id UUID REFERENCES sessoes_caixa(id);
CREATE INDEX idx_pedidos_sessao_caixa ON pedidos (sessao_caixa_id) WHERE sessao_caixa_id IS NOT NULL;

-- Conferir fechamentos e definir o fundo de troco: dado de dinheiro, fica com o Financeiro.
INSERT INTO cargo_permissoes (cargo_id, permissao)
SELECT id, 'CAIXA_CONFERIR' FROM cargos WHERE nome = 'Financeiro';

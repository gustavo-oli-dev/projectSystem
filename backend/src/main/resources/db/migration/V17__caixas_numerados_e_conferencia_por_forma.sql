-- Caixas numerados e conferência por forma de pagamento (D29).
-- O caixa é o ponto físico (Caixa 01, 02...); cada abertura liga um caixa a um operador.

CREATE TABLE pontos_caixa (
    id UUID PRIMARY KEY,
    numero INTEGER NOT NULL UNIQUE CHECK (numero BETWEEN 1 AND 999),
    ativo BOOLEAN NOT NULL,
    criado_em TIMESTAMPTZ NOT NULL
);

-- Sessões antigas (antes dos caixas numerados) ficam sem ponto.
ALTER TABLE sessoes_caixa ADD COLUMN ponto_caixa_id UUID REFERENCES pontos_caixa(id);
-- Um caixa físico só pode ter uma abertura por vez (além de um caixa aberto por operador).
CREATE UNIQUE INDEX uq_sessoes_caixa_uma_aberta_por_ponto ON sessoes_caixa (ponto_caixa_id)
    WHERE status = 'ABERTA' AND ponto_caixa_id IS NOT NULL;

-- No fechamento: cartão e Pix da maquininha conferidos com o relatório dela (valor do sistema × informado).
CREATE TABLE conferencias_forma_caixa (
    sessao_caixa_id UUID NOT NULL REFERENCES sessoes_caixa(id),
    forma VARCHAR(20) NOT NULL,
    valor_sistema NUMERIC(14, 2) NOT NULL CHECK (valor_sistema >= 0),
    valor_informado NUMERIC(14, 2) NOT NULL CHECK (valor_informado >= 0),
    PRIMARY KEY (sessao_caixa_id, forma)
);

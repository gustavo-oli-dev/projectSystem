-- Promoções (D38): preço de oferta com período e "leve X pague Y", aplicadas sozinhas na venda.

CREATE TABLE promocoes (
    id UUID PRIMARY KEY,
    produto_id UUID NOT NULL REFERENCES produtos(id),
    tipo VARCHAR(20) NOT NULL,
    preco_oferta NUMERIC(14, 2),
    leve INTEGER,
    pague INTEGER,
    inicio DATE NOT NULL,
    fim DATE NOT NULL,
    encerrada_em TIMESTAMPTZ,
    encerrada_por VARCHAR(255),
    criada_por VARCHAR(255) NOT NULL,
    criada_em TIMESTAMPTZ NOT NULL,
    CHECK (fim >= inicio),
    CHECK ((tipo = 'PRECO_OFERTA' AND preco_oferta > 0 AND leve IS NULL AND pague IS NULL)
        OR (tipo = 'LEVE_PAGUE' AND preco_oferta IS NULL AND pague >= 1 AND leve > pague))
);
CREATE INDEX idx_promocoes_produto_periodo ON promocoes (produto_id, inicio, fim);

-- Parte do desconto do item que veio da promoção. A coluna desconto continua sendo o desconto total
-- do item (promoção + desconto do gerente), então os relatórios que já subtraem "desconto" seguem certos.
ALTER TABLE itens_pedido ADD COLUMN desconto_promocao NUMERIC(14, 2) NOT NULL DEFAULT 0 CHECK (desconto_promocao >= 0);
ALTER TABLE itens_pedido ADD CONSTRAINT itens_pedido_desconto_inclui_promocao CHECK (desconto >= desconto_promocao);

-- Quem cadastra preços também cuida das promoções.
INSERT INTO cargo_permissoes (cargo_id, permissao)
SELECT cargo_id, 'PROMOCOES_GERENCIAR' FROM cargo_permissoes WHERE permissao = 'CATALOGO_GERENCIAR';

-- Caixa (PDV): venda presencial com consumidor não identificado, NFC-e e pagamento na maquininha (D19).

ALTER TABLE pedidos ADD COLUMN canal VARCHAR(20) NOT NULL DEFAULT 'PAINEL';
ALTER TABLE pedidos ALTER COLUMN canal DROP DEFAULT;
ALTER TABLE pedidos ADD COLUMN cpf_na_nota VARCHAR(11);
ALTER TABLE pedidos ALTER COLUMN cliente_id DROP NOT NULL;
-- Só a venda de balcão pode não ter cliente cadastrado.
ALTER TABLE pedidos ADD CONSTRAINT pedidos_cliente_ou_balcao CHECK (cliente_id IS NOT NULL OR canal = 'BALCAO');
CREATE INDEX idx_pedidos_canal_criado_em ON pedidos (canal, criado_em DESC);

CREATE TABLE pagamentos_presenciais (
    id UUID PRIMARY KEY,
    pedido_id UUID NOT NULL UNIQUE REFERENCES pedidos(id),
    forma VARCHAR(20) NOT NULL,
    valor NUMERIC(14, 2) NOT NULL CHECK (valor >= 0),
    valor_recebido NUMERIC(14, 2),
    bandeira VARCHAR(20),
    codigo_autorizacao VARCHAR(20),
    maquininha_integrada BOOLEAN NOT NULL,
    status VARCHAR(20) NOT NULL,
    operador VARCHAR(255) NOT NULL,
    criado_em TIMESTAMPTZ NOT NULL,
    estornado_em TIMESTAMPTZ
);

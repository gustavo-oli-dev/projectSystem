CREATE TABLE cobrancas (
    id UUID PRIMARY KEY,
    pedido_id UUID NOT NULL REFERENCES pedidos(id),
    meio VARCHAR(10) NOT NULL,
    valor NUMERIC(14,2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    referencia_externa VARCHAR(60) NOT NULL UNIQUE,
    criado_em TIMESTAMPTZ NOT NULL,
    atualizado_em TIMESTAMPTZ NOT NULL
);

CREATE TABLE eventos_webhook_pagamento (
    id UUID PRIMARY KEY,
    referencia_externa VARCHAR(60) NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL,
    recebido_em TIMESTAMPTZ NOT NULL,
    processado_em TIMESTAMPTZ
);

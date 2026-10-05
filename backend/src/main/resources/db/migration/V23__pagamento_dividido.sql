-- Pagamento dividido no caixa (D36): uma venda pode ter mais de um pagamento (ex.: parte em
-- dinheiro, parte no cartão). A soma dos pagamentos é o total da venda.
ALTER TABLE pagamentos_presenciais DROP CONSTRAINT IF EXISTS pagamentos_presenciais_pedido_id_key;
CREATE INDEX IF NOT EXISTS idx_pagamentos_presenciais_pedido ON pagamentos_presenciais (pedido_id);

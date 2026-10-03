-- Maquininha integrada ao caixa (D20): o valor vai para a maquininha e o resultado volta sozinho.
ALTER TABLE pagamentos_presenciais ADD COLUMN id_transacao_maquininha VARCHAR(100);
ALTER TABLE pagamentos_presenciais ADD COLUMN id_pagamento_provedor VARCHAR(100);
ALTER TABLE pagamentos_presenciais ALTER COLUMN codigo_autorizacao TYPE VARCHAR(40);

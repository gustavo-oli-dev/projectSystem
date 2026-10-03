-- Dois números de WhatsApp (D17): cada evento registra em qual canal chegou.
-- Eventos antigos vieram todos do número da empresa (atendimento).
ALTER TABLE eventos_webhook_whatsapp ADD COLUMN canal VARCHAR(20) NOT NULL DEFAULT 'ATENDIMENTO';
ALTER TABLE eventos_webhook_whatsapp ALTER COLUMN canal DROP DEFAULT;

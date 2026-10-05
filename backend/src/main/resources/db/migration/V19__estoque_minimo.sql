-- Estoque mínimo por produto (D33): abaixo dele o produto entra na sugestão de compra.
-- Vazio = não definido (vale o aviso padrão de 5 unidades).
ALTER TABLE produtos ADD COLUMN estoque_minimo INTEGER CHECK (estoque_minimo >= 0);

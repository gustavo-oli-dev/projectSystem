-- Tributação por produto (D42): o que a NFC-e/NF-e precisa de cada item além do NCM. Tudo vazio =
-- tributação ainda não definida (o regime tributário da empresa segue em aberto: CST ou CSOSN).
ALTER TABLE produtos ADD COLUMN origem_mercadoria INTEGER CHECK (origem_mercadoria BETWEEN 0 AND 8);
ALTER TABLE produtos ADD COLUMN cst_icms VARCHAR(3);
ALTER TABLE produtos ADD COLUMN aliquota_icms NUMERIC(5, 2) CHECK (aliquota_icms >= 0 AND aliquota_icms <= 100);
ALTER TABLE produtos ADD COLUMN substituicao_tributaria BOOLEAN;
ALTER TABLE produtos ADD COLUMN cest VARCHAR(7);
ALTER TABLE produtos ADD COLUMN cesta_basica BOOLEAN;
ALTER TABLE produtos ADD COLUMN classificacao_tributaria VARCHAR(6);

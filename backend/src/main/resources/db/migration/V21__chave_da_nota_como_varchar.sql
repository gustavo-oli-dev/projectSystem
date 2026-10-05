-- A chave de acesso tem sempre 44 dígitos, mas a entidade a mapeia como texto comum (VARCHAR).
ALTER TABLE notas_entrada ALTER COLUMN chave_acesso TYPE VARCHAR(44);

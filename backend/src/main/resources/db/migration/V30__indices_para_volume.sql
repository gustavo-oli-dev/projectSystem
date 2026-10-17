-- Com anos de vendas, estas buscas viravam varredura da tabela inteira.

-- Itens de um pedido (detalhe do pedido, últimas vendas do caixa).
CREATE INDEX idx_itens_pedido_pedido ON itens_pedido (pedido_id);

-- Documentos fiscais de um pedido e pedidos ainda sem documento.
CREATE INDEX idx_documentos_fiscais_pedido ON documentos_fiscais (pedido_id);

-- Lista de documentos fiscais mais recentes.
CREATE INDEX idx_documentos_fiscais_atualizado_em ON documentos_fiscais (atualizado_em DESC);

-- Pedidos mais recentes (todos os canais) e pedidos de um cliente.
CREATE INDEX idx_pedidos_criado_em ON pedidos (criado_em DESC);
CREATE INDEX idx_pedidos_cliente ON pedidos (cliente_id, criado_em DESC) WHERE cliente_id IS NOT NULL;

-- Pedidos aguardando emissão (aba Fiscal).
CREATE INDEX idx_pedidos_status ON pedidos (status, criado_em DESC);

-- Cobranças de um pedido.
CREATE INDEX idx_cobrancas_pedido ON cobrancas (pedido_id);

package com.empresax.sistema.produto.estoque;

public enum TipoMovimentacaoEstoque {
    /** Mercadoria que chegou (compra, reposição). */
    ENTRADA,
    /** Saída por venda confirmada. */
    VENDA,
    /** Volta ao estoque por venda cancelada ou reembolsada. */
    DEVOLUCAO
}

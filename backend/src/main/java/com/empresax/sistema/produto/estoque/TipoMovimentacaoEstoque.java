package com.empresax.sistema.produto.estoque;

public enum TipoMovimentacaoEstoque {
    /** Mercadoria que chegou (compra, reposição). */
    ENTRADA(false),
    /** Saída por venda confirmada. */
    VENDA(true),
    /** Volta ao estoque por venda cancelada ou reembolsada. */
    DEVOLUCAO(false),
    /** Saiu sem ser vendido: vencido, avariado, furto, uso interno (com motivo). */
    PERDA(true),
    /** Contagem do inventário achou mais do que o sistema tinha. */
    INVENTARIO_SOBRA(false),
    /** Contagem do inventário achou menos do que o sistema tinha. */
    INVENTARIO_FALTA(true);

    private final boolean saida;

    TipoMovimentacaoEstoque(boolean saida) {
        this.saida = saida;
    }

    /** true = tirou do estoque (o histórico mostra "−"). */
    public boolean saida() {
        return saida;
    }
}

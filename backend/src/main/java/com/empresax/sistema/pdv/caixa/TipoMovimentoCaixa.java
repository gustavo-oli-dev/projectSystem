package com.empresax.sistema.pdv.caixa;

/** Dinheiro que entra ou sai da gaveta fora de uma venda. */
public enum TipoMovimentoCaixa {
    /** Reposição de troco: notas trocadas colocadas na gaveta durante o dia. */
    SUPRIMENTO,
    /** Retirada: dinheiro levado ao cofre, ou devolvido ao cliente num cancelamento. */
    SANGRIA
}

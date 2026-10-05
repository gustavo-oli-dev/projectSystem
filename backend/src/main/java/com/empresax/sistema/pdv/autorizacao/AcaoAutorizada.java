package com.empresax.sistema.pdv.autorizacao;

/** O que o gerente pode liberar no caixa com a senha dele (D35). */
public enum AcaoAutorizada {
    /** Desconto na venda. */
    DESCONTO,
    /** Tirar da venda um item que já foi lido. */
    CANCELAR_ITEM
}

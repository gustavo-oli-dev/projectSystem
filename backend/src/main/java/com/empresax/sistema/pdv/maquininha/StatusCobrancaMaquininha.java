package com.empresax.sistema.pdv.maquininha;

public enum StatusCobrancaMaquininha {
    /** Na tela da maquininha, esperando o cliente passar o cartão. */
    AGUARDANDO,
    APROVADA,
    /** Cartão recusado ou erro na transação: dá para tentar de novo. */
    RECUSADA,
    /** Cancelada na própria maquininha ou expirou. */
    CANCELADA
}

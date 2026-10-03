package com.empresax.sistema.pdv;

public enum StatusPagamentoPresencial {
    /** Valor enviado para a maquininha integrada, esperando o cliente passar o cartão. */
    AGUARDANDO_MAQUININHA,
    /** Cartão recusado na maquininha: dá para tentar de novo ou cancelar a venda. */
    RECUSADO,
    APROVADO,
    /** Venda desistida antes de pagar (nada foi cobrado). */
    CANCELADO,
    /** Venda cancelada depois de paga: valor devolvido (estorno ou dinheiro devolvido). */
    ESTORNADO
}

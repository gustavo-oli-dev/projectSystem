package com.empresax.sistema.cobranca;

public enum StatusCobranca {
    PENDENTE,
    PAGA,
    VENCIDA,
    CANCELADA,
    /** Estava paga e o dinheiro foi devolvido ao cliente. */
    REEMBOLSADA
}

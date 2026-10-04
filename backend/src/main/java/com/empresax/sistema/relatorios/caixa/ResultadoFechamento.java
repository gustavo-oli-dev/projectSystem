package com.empresax.sistema.relatorios.caixa;

import java.math.BigDecimal;

public enum ResultadoFechamento {
    ABERTO,
    BATEU,
    SOBROU,
    FALTOU;

    static ResultadoFechamento de(BigDecimal diferenca) {
        int sinal = diferenca.signum();
        if (sinal == 0) {
            return BATEU;
        }
        return sinal > 0 ? SOBROU : FALTOU;
    }
}

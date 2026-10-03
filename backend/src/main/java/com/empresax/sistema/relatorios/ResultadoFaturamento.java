package com.empresax.sistema.relatorios;

import java.math.BigDecimal;

public record ResultadoFaturamento(BigDecimal total, int quantidadeCobrancas) {
}

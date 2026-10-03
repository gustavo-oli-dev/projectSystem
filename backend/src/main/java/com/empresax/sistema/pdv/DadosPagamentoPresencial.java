package com.empresax.sistema.pdv;

import java.math.BigDecimal;

/** O que o operador informa ao fechar a venda; cada forma usa só os campos que lhe cabem. */
public record DadosPagamentoPresencial(
        FormaPagamentoPresencial forma,
        BigDecimal valorRecebido,
        BandeiraCartao bandeira,
        String codigoAutorizacao
) {
}

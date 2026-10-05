package com.empresax.sistema.pdv;

import java.math.BigDecimal;

/**
 * O que o operador informa de um pagamento; cada forma usa só os campos que lhe cabem.
 * valor: quanto esta parte paga (pagamento dividido); vazio = o que falta para fechar a venda.
 */
public record DadosPagamentoPresencial(
        FormaPagamentoPresencial forma,
        BigDecimal valor,
        BigDecimal valorRecebido,
        BandeiraCartao bandeira,
        String codigoAutorizacao
) {
}

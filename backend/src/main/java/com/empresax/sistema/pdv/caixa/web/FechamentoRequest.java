package com.empresax.sistema.pdv.caixa.web;

import com.empresax.sistema.pdv.FormaPagamentoPresencial;
import com.empresax.sistema.pdv.caixa.Cedula;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Fechamento (cego): contagem da gaveta por cédula, o que o relatório da maquininha mostra em cada
 * forma (crédito, débito, Pix) e uma observação opcional.
 */
public record FechamentoRequest(
        @NotNull(message = "Informe a contagem da gaveta")
        Map<Cedula, @NotNull @PositiveOrZero @Max(value = CedulasRequest.QUANTIDADE_MAXIMA, message = "Quantidade de cédulas fora do limite") Integer> cedulas,
        @NotNull(message = "Informe os valores do relatório da maquininha")
        Map<FormaPagamentoPresencial, @NotNull @PositiveOrZero @Digits(integer = 12, fraction = 2, message = "Valor inválido") BigDecimal> maquininha,
        @Size(max = 500, message = "Observação muito longa") String observacao
) {
}

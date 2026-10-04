package com.empresax.sistema.pdv.caixa.web;

import com.empresax.sistema.pdv.caixa.Cedula;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.util.Map;

/** Contagem da gaveta no fechamento (feita às cegas) e uma observação opcional. */
public record FechamentoRequest(
        @NotNull(message = "Informe a contagem da gaveta")
        Map<Cedula, @NotNull @PositiveOrZero @Max(value = CedulasRequest.QUANTIDADE_MAXIMA, message = "Quantidade de cédulas fora do limite") Integer> cedulas,
        @Size(max = 500, message = "Observação muito longa") String observacao
) {
}

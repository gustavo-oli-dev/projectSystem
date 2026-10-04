package com.empresax.sistema.pdv.caixa.web;

import com.empresax.sistema.pdv.caixa.Cedula;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.Map;
import java.util.UUID;

/** Abertura feita por quem gerencia o caixa: para qual operador e com quais cédulas. */
public record AberturaCaixaRequest(
        @NotNull(message = "Escolha o operador do caixa") UUID operadorId,
        @NotNull(message = "Informe a contagem das cédulas")
        Map<Cedula, @NotNull @PositiveOrZero @Max(value = CedulasRequest.QUANTIDADE_MAXIMA, message = "Quantidade de cédulas fora do limite") Integer> cedulas
) {
}

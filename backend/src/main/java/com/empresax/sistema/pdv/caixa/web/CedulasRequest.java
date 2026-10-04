package com.empresax.sistema.pdv.caixa.web;

import com.empresax.sistema.pdv.caixa.Cedula;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.Map;

/** Abertura do caixa e fundo de troco padrão: quantas cédulas de cada valor. */
public record CedulasRequest(
        @NotNull(message = "Informe a contagem das cédulas")
        Map<Cedula, @NotNull @PositiveOrZero @Max(value = CedulasRequest.QUANTIDADE_MAXIMA, message = "Quantidade de cédulas fora do limite") Integer> cedulas
) {

    static final int QUANTIDADE_MAXIMA = 100_000;
}

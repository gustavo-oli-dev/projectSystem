package com.empresax.sistema.pdv.caixa.web;

import com.empresax.sistema.pdv.caixa.Cedula;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.util.Map;

/** Reposição de troco: as cédulas que entraram na gaveta. */
public record SuprimentoRequest(
        @NotNull(message = "Informe as cédulas da reposição")
        Map<Cedula, @NotNull @PositiveOrZero @Max(value = CedulasRequest.QUANTIDADE_MAXIMA, message = "Quantidade de cédulas fora do limite") Integer> cedulas,
        @NotBlank(message = "Informe o motivo") @Size(max = 200, message = "Motivo muito longo") String motivo
) {
}

package com.empresax.sistema.pdv.caixa.web;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record PontoCaixaRequest(
        @NotNull(message = "Informe o número do caixa")
        @Min(value = 1, message = "O número do caixa vai de 1 a 999")
        @Max(value = 999, message = "O número do caixa vai de 1 a 999") Integer numero
) {
}

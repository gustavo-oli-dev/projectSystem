package com.empresax.sistema.pdv.caixa.web;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record SangriaRequest(
        @NotNull(message = "Informe o valor da sangria") @Positive(message = "O valor da sangria precisa ser maior que zero")
        @Digits(integer = 12, fraction = 2, message = "Valor inválido") BigDecimal valor,
        @NotBlank(message = "Informe o motivo") @Size(max = 200, message = "Motivo muito longo") String motivo
) {
}

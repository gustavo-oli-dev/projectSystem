package com.empresax.sistema.pdv.web;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/** Desconto na venda em reais, com o token da autorização do gerente. */
public record DescontoVendaRequest(
        @NotNull(message = "Informe o valor do desconto") @Positive(message = "O desconto precisa ser maior que zero")
        @Digits(integer = 12, fraction = 2, message = "Desconto inválido") BigDecimal valor,
        @NotBlank(message = "Desconto precisa da autorização de um gerente") String tokenAutorizacao
) {
}

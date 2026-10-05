package com.empresax.sistema.compras.contas.web;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** Despesa lançada à mão; o contato (fornecedor, transportadora...) é opcional. */
public record ContaAPagarRequest(
        UUID contatoId,
        @NotBlank(message = "Informe a descrição da conta") @Size(max = 200, message = "Descrição muito longa") String descricao,
        @NotNull(message = "Informe o valor") @Positive(message = "O valor precisa ser maior que zero")
        @Digits(integer = 12, fraction = 2, message = "Valor inválido") BigDecimal valor,
        @NotNull(message = "Informe o vencimento") LocalDate vencimento
) {
}

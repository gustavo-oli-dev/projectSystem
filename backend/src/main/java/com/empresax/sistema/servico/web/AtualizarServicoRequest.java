package com.empresax.sistema.servico.web;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record AtualizarServicoRequest(
        @NotBlank(message = "Nome é obrigatório") String nome,
        String descricao,
        @NotNull(message = "Preço unitário é obrigatório")
        @DecimalMin(value = "0.0", message = "Preço unitário não pode ser negativo") BigDecimal precoUnitario
) {
}

package com.empresax.sistema.produto.web;

import com.empresax.sistema.produto.estoque.MotivoPerda;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PerdaEstoqueRequest(
        @Min(value = 1, message = "Quantidade deve ser maior que zero")
        @Max(value = 100_000, message = "Quantidade muito alta para uma perda só") int quantidade,
        @NotNull(message = "Informe o motivo da perda") MotivoPerda motivo,
        @Size(max = 200, message = "Observação muito longa") String observacao
) {
}

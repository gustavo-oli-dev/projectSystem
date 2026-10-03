package com.empresax.sistema.produto.web;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record EntradaEstoqueRequest(
        @Min(value = 1, message = "Quantidade deve ser maior que zero")
        @Max(value = 100_000, message = "Quantidade muito alta para uma entrada só") int quantidade
) {
}

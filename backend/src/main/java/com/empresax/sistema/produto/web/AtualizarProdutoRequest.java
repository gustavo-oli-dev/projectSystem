package com.empresax.sistema.produto.web;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record AtualizarProdutoRequest(
        @NotBlank(message = "Nome é obrigatório") String nome,
        String descricao,
        @NotNull(message = "Preço unitário é obrigatório")
        @DecimalMin(value = "0.0", message = "Preço unitário não pode ser negativo") BigDecimal precoUnitario,
        @Size(max = 14, message = "Código de barras tem no máximo 14 dígitos") String codigoBarras,
        @DecimalMin(value = "0.0", message = "Custo não pode ser negativo") BigDecimal custoUnitario
) {
}

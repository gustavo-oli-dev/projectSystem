package com.empresax.sistema.produto.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

/** Contagem do inventário: quantos de cada produto foram contados na prateleira. */
public record InventarioRequest(
        @NotEmpty(message = "Informe a contagem de ao menos um produto") @Size(max = 5000, message = "Produtos demais de uma vez")
        List<@Valid @NotNull ContagemProduto> contagens
) {

    public record ContagemProduto(
            @NotNull(message = "Produto da contagem não informado") UUID produtoId,
            @Min(value = 0, message = "A quantidade contada não pode ser negativa")
            @Max(value = 1_000_000, message = "Quantidade contada fora do limite") int quantidadeContada
    ) {
    }
}

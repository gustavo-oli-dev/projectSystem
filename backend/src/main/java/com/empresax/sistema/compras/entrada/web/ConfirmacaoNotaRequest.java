package com.empresax.sistema.compras.entrada.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

/** A decisão da tela: qual item da nota é qual produto (os ausentes são ignorados) e se o custo muda. */
public record ConfirmacaoNotaRequest(
        @NotNull(message = "Informe os itens da nota") @Size(max = 990, message = "Itens demais")
        List<@Valid @NotNull ItemLigado> itens,
        boolean atualizarCusto
) {

    public record ItemLigado(
            @NotNull(message = "Item sem número") Integer ordem,
            @NotNull(message = "Escolha o produto do item") UUID produtoId
    ) {
    }
}

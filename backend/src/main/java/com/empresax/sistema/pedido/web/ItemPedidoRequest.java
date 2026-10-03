package com.empresax.sistema.pedido.web;

import com.empresax.sistema.pedido.TipoItem;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

public record ItemPedidoRequest(
        @NotNull(message = "Tipo do item (produto ou serviço) é obrigatório") TipoItem tipo,
        @NotNull(message = "Referência do item é obrigatória") UUID referenciaId,
        @Positive(message = "Quantidade deve ser maior que zero") int quantidade
) {
}

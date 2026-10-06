package com.empresax.sistema.pedido.web;

import com.empresax.sistema.pedido.ItemPedido;
import com.empresax.sistema.pedido.TipoItem;

import java.math.BigDecimal;
import java.util.UUID;

public record ItemPedidoResponse(
        TipoItem tipo,
        UUID referenciaId,
        String descricao,
        BigDecimal precoUnitario,
        int quantidade,
        /** Quanto este item ganhou na promoção do produto (zero = sem promoção). */
        BigDecimal descontoPromocao,
        BigDecimal subtotal
) {

    public static ItemPedidoResponse de(ItemPedido item) {
        return new ItemPedidoResponse(
                item.tipo(),
                item.referenciaId(),
                item.descricao(),
                item.precoUnitario().valor(),
                item.quantidade(),
                item.descontoPromocao().valor(),
                item.subtotal().valor()
        );
    }
}

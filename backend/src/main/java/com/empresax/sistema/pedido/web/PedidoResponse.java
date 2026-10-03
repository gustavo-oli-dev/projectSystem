package com.empresax.sistema.pedido.web;

import com.empresax.sistema.pedido.CanalVenda;
import com.empresax.sistema.pedido.Pedido;
import com.empresax.sistema.pedido.StatusPedido;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** clienteId e cpfNaNota vêm nulos na venda de balcão sem consumidor identificado. */
public record PedidoResponse(
        UUID id,
        UUID clienteId,
        CanalVenda canal,
        String cpfNaNota,
        StatusPedido status,
        List<ItemPedidoResponse> itens,
        BigDecimal valorTotal,
        Instant criadoEm
) {

    public static PedidoResponse de(Pedido pedido) {
        return new PedidoResponse(
                pedido.id(),
                pedido.clienteId().orElse(null),
                pedido.canal(),
                pedido.cpfNaNota().orElse(null),
                pedido.status(),
                pedido.itens().stream().map(ItemPedidoResponse::de).toList(),
                pedido.valorTotal().valor(),
                pedido.criadoEm()
        );
    }
}

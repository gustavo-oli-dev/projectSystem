package com.empresax.sistema.pedido;

import java.util.UUID;

/**
 * Entrada para montar um ItemPedido: o chamador informa o que quer comprar (tipo + referência +
 * quantidade); o PedidoService busca o preço e a descrição atuais do Produto/Servico e os
 * congela no ItemPedido no momento da criação.
 */
public record ItemPedidoRequerido(TipoItem tipo, UUID referenciaId, int quantidade) {
}

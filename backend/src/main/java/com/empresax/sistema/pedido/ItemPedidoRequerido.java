package com.empresax.sistema.pedido;

import java.util.UUID;

/**
 * Entrada para montar um ItemPedido: o chamador informa o que quer comprar (tipo + referência +
 * quantidade); o PedidoService busca o preço e a descrição atuais do Produto/Servico e os
 * congela no ItemPedido no momento da criação.
 *
 * @param embalagemId produto vendido numa embalagem (ex.: fardo com 12) — D41; nulo = unidade avulsa.
 */
public record ItemPedidoRequerido(TipoItem tipo, UUID referenciaId, int quantidade, UUID embalagemId) {

    public ItemPedidoRequerido(TipoItem tipo, UUID referenciaId, int quantidade) {
        this(tipo, referenciaId, quantidade, null);
    }
}

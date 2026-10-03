package com.empresax.sistema.pdv;

import com.empresax.sistema.pedido.ItemPedidoRequerido;

import java.util.List;
import java.util.UUID;

/** O que toda venda do caixa informa: produtos, CPF na nota (opcional) e cliente cadastrado (opcional). */
public record DadosVendaBalcao(List<ItemPedidoRequerido> itens, String cpfNaNota, UUID clienteId) {
}

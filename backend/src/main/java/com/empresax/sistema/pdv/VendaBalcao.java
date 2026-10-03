package com.empresax.sistema.pdv;

import com.empresax.sistema.pedido.Pedido;

/** Uma venda do caixa: o pedido (itens, estoque, nota) e como foi paga. */
public record VendaBalcao(Pedido pedido, PagamentoPresencial pagamento) {
}

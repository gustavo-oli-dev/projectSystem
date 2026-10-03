package com.empresax.sistema.pdv.web;

import com.empresax.sistema.pdv.PagamentoPresencial;
import com.empresax.sistema.pdv.VendaBalcao;
import com.empresax.sistema.pedido.web.ItemPedidoResponse;
import com.empresax.sistema.shared.dinheiro.Dinheiro;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record VendaBalcaoResponse(
        UUID pedidoId,
        String status,
        List<ItemPedidoResponse> itens,
        BigDecimal total,
        String cpfNaNota,
        String formaPagamento,
        String bandeira,
        String codigoAutorizacao,
        boolean maquininhaIntegrada,
        BigDecimal valorRecebido,
        BigDecimal troco,
        String statusPagamento,
        String operador,
        Instant criadaEm
) {

    public static VendaBalcaoResponse de(VendaBalcao venda) {
        PagamentoPresencial pagamento = venda.pagamento();
        return new VendaBalcaoResponse(
                venda.pedido().id(),
                venda.pedido().status().name(),
                venda.pedido().itens().stream().map(ItemPedidoResponse::de).toList(),
                venda.pedido().valorTotal().valor(),
                venda.pedido().cpfNaNota().orElse(null),
                pagamento.forma().name(),
                pagamento.bandeira().map(Enum::name).orElse(null),
                pagamento.codigoAutorizacao().orElse(null),
                pagamento.maquininhaIntegrada(),
                pagamento.valorRecebido().map(Dinheiro::valor).orElse(null),
                pagamento.troco().map(Dinheiro::valor).orElse(null),
                pagamento.status().name(),
                pagamento.operador(),
                pagamento.criadoEm());
    }
}

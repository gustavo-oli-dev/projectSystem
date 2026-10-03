package com.empresax.sistema.pdv.web;

import com.empresax.sistema.cobranca.Cobranca;
import com.empresax.sistema.cobranca.StatusCobranca;
import com.empresax.sistema.pdv.PagamentoPresencial;
import com.empresax.sistema.pdv.StatusPagamentoPresencial;
import com.empresax.sistema.pdv.VendaBalcao;
import com.empresax.sistema.pedido.Pedido;
import com.empresax.sistema.pedido.web.ItemPedidoResponse;
import com.empresax.sistema.shared.dinheiro.Dinheiro;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * formaPagamento: DINHEIRO, CARTAO_CREDITO, CARTAO_DEBITO, PIX (maquininha) ou PIX_QR (QR na tela).
 * statusPagamento: AGUARDANDO (Pix ou maquininha esperando o cliente), RECUSADO (cartão recusado na
 * maquininha — dá para tentar de novo), APROVADO ou ESTORNADO.
 */
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

    private static final String FORMA_PIX_NA_TELA = "PIX_QR";
    private static final String AGUARDANDO = "AGUARDANDO";
    private static final String APROVADO = "APROVADO";
    private static final String RECUSADO = "RECUSADO";
    private static final String ESTORNADO = "ESTORNADO";

    public static VendaBalcaoResponse de(VendaBalcao venda) {
        return venda.pagamentoPresencial()
                .map(pagamento -> dePresencial(venda.pedido(), pagamento))
                .orElseGet(() -> dePix(venda.pedido(), venda.cobrancaPix().orElseThrow()));
    }

    private static VendaBalcaoResponse dePresencial(Pedido pedido, PagamentoPresencial pagamento) {
        return new VendaBalcaoResponse(
                pedido.id(), pedido.status().name(), itens(pedido), pedido.valorTotal().valor(),
                pedido.cpfNaNota().orElse(null),
                pagamento.forma().name(),
                pagamento.bandeira().map(Enum::name).orElse(null),
                pagamento.codigoAutorizacao().orElse(null),
                pagamento.maquininhaIntegrada(),
                pagamento.valorRecebido().map(Dinheiro::valor).orElse(null),
                pagamento.troco().map(Dinheiro::valor).orElse(null),
                statusPresencial(pagamento.status()),
                pagamento.operador(),
                pagamento.criadoEm());
    }

    private static VendaBalcaoResponse dePix(Pedido pedido, Cobranca cobranca) {
        return new VendaBalcaoResponse(
                pedido.id(), pedido.status().name(), itens(pedido), pedido.valorTotal().valor(),
                pedido.cpfNaNota().orElse(null),
                FORMA_PIX_NA_TELA, null, null, false, null, null,
                statusDoPix(cobranca.status()),
                null,
                cobranca.criadoEm());
    }

    private static String statusPresencial(StatusPagamentoPresencial status) {
        return switch (status) {
            case AGUARDANDO_MAQUININHA -> AGUARDANDO;
            case RECUSADO -> RECUSADO;
            case APROVADO -> APROVADO;
            case CANCELADO, ESTORNADO -> ESTORNADO;
        };
    }

    private static String statusDoPix(StatusCobranca status) {
        return switch (status) {
            case PENDENTE -> AGUARDANDO;
            case PAGA -> APROVADO;
            case VENCIDA, CANCELADA, REEMBOLSADA -> ESTORNADO;
        };
    }

    private static List<ItemPedidoResponse> itens(Pedido pedido) {
        return pedido.itens().stream().map(ItemPedidoResponse::de).toList();
    }
}

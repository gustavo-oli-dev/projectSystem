package com.empresax.sistema.pdv;

import com.empresax.sistema.cobranca.Cobranca;
import com.empresax.sistema.pedido.Pedido;

import java.util.Optional;

/**
 * Uma venda do caixa: o pedido (itens, estoque, nota) e como foi paga — ou pagamento presencial
 * (dinheiro, maquininha), ou Pix com QR code na tela (cobrança no Mercado Pago). Sempre um dos dois.
 */
public final class VendaBalcao {

    private final Pedido pedido;
    private final PagamentoPresencial pagamentoPresencial;
    private final Cobranca cobrancaPix;

    private VendaBalcao(Pedido pedido, PagamentoPresencial pagamentoPresencial, Cobranca cobrancaPix) {
        this.pedido = pedido;
        this.pagamentoPresencial = pagamentoPresencial;
        this.cobrancaPix = cobrancaPix;
    }

    public static VendaBalcao presencial(Pedido pedido, PagamentoPresencial pagamento) {
        return new VendaBalcao(pedido, pagamento, null);
    }

    public static VendaBalcao porPixNaTela(Pedido pedido, Cobranca cobranca) {
        return new VendaBalcao(pedido, null, cobranca);
    }

    public Pedido pedido() {
        return pedido;
    }

    public Optional<PagamentoPresencial> pagamentoPresencial() {
        return Optional.ofNullable(pagamentoPresencial);
    }

    /** E-mail de quem registrou a venda no caixa. Pix com QR na tela não guarda o operador. */
    public Optional<String> operador() {
        return pagamentoPresencial().map(PagamentoPresencial::operador);
    }

    public Optional<Cobranca> cobrancaPix() {
        return Optional.ofNullable(cobrancaPix);
    }
}

package com.empresax.sistema.pdv;

import com.empresax.sistema.cobranca.Cobranca;
import com.empresax.sistema.pedido.Pedido;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Uma venda do caixa: o pedido (itens, estoque, nota) e como foi paga — ou pagamentos presenciais
 * (dinheiro, maquininha; mais de um no pagamento dividido), ou Pix com QR code na tela (cobrança no
 * Mercado Pago). Sempre um dos dois.
 */
public final class VendaBalcao {

    private final Pedido pedido;
    private final List<PagamentoPresencial> pagamentos;
    private final Cobranca cobrancaPix;

    private VendaBalcao(Pedido pedido, List<PagamentoPresencial> pagamentos, Cobranca cobrancaPix) {
        this.pedido = pedido;
        this.pagamentos = List.copyOf(pagamentos);
        this.cobrancaPix = cobrancaPix;
    }

    public static VendaBalcao presencial(Pedido pedido, List<PagamentoPresencial> pagamentos) {
        if (pagamentos.isEmpty()) {
            throw new IllegalArgumentException("Venda presencial sem pagamento");
        }
        return new VendaBalcao(pedido, pagamentos, null);
    }

    public static VendaBalcao porPixNaTela(Pedido pedido, Cobranca cobranca) {
        return new VendaBalcao(pedido, List.of(), cobranca);
    }

    public Pedido pedido() {
        return pedido;
    }

    /** Vazio = Pix com QR na tela. Mais de um = pagamento dividido (na ordem em que foram feitos). */
    public List<PagamentoPresencial> pagamentos() {
        return pagamentos;
    }

    public boolean presencial() {
        return !pagamentos.isEmpty();
    }

    /** E-mail de quem registrou a venda no caixa. Pix com QR na tela não guarda o operador. */
    public Optional<String> operador() {
        return pagamentos.stream().findFirst().map(PagamentoPresencial::operador);
    }

    /** E-mails de quem aparece na venda (operador e gerente do desconto), para resolver os nomes de uma vez. */
    public Set<String> pessoas() {
        Set<String> emails = new HashSet<>();
        operador().ifPresent(emails::add);
        pedido.descontoAutorizadoPor().ifPresent(emails::add);
        return emails;
    }

    public Optional<Cobranca> cobrancaPix() {
        return Optional.ofNullable(cobrancaPix);
    }
}

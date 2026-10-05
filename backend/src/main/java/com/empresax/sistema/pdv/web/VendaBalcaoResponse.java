package com.empresax.sistema.pdv.web;

import com.empresax.sistema.cobranca.Cobranca;
import com.empresax.sistema.cobranca.StatusCobranca;
import com.empresax.sistema.pdv.FormaPagamentoPresencial;
import com.empresax.sistema.pdv.PagamentoPresencial;
import com.empresax.sistema.pdv.StatusPagamentoPresencial;
import com.empresax.sistema.pdv.VendaBalcao;
import com.empresax.sistema.pedido.Pedido;
import com.empresax.sistema.pedido.web.ItemPedidoResponse;
import com.empresax.sistema.shared.dinheiro.Dinheiro;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * formaPagamento: DINHEIRO, CARTAO_CREDITO, CARTAO_DEBITO, PIX (maquininha), PIX_QR (QR na tela) ou
 * DIVIDIDO (mais de uma forma — o detalhe vem em pagamentos).
 * statusPagamento: AGUARDANDO (Pix ou maquininha esperando o cliente), RECUSADO (cartão recusado na
 * maquininha — dá para tentar de novo), APROVADO ou ESTORNADO. No dividido, vale o da parte pendente.
 */
public record VendaBalcaoResponse(
        UUID pedidoId,
        String status,
        List<ItemPedidoResponse> itens,
        BigDecimal total,
        /** Desconto autorizado por um gerente (zero = sem desconto); o total já vem com ele. */
        BigDecimal desconto,
        String cpfNaNota,
        String formaPagamento,
        String bandeira,
        String codigoAutorizacao,
        boolean maquininhaIntegrada,
        BigDecimal valorRecebido,
        BigDecimal troco,
        String statusPagamento,
        String operador,
        /** Nome de quem vendeu; null se a venda não registra operador ou o usuário não existe mais. */
        String operadorNome,
        Instant criadaEm,
        /** Cada forma usada, na ordem; uma só quando não foi dividido; vazio no Pix com QR na tela. */
        List<ParteResponse> pagamentos
) {

    private static final String FORMA_PIX_NA_TELA = "PIX_QR";
    private static final String FORMA_DIVIDIDO = "DIVIDIDO";
    private static final String AGUARDANDO = "AGUARDANDO";
    private static final String APROVADO = "APROVADO";
    private static final String RECUSADO = "RECUSADO";
    private static final String ESTORNADO = "ESTORNADO";

    public record ParteResponse(
            String forma, BigDecimal valor, String bandeira, String codigoAutorizacao,
            boolean maquininhaIntegrada, BigDecimal valorRecebido, BigDecimal troco, String status
    ) {
        static ParteResponse de(PagamentoPresencial pagamento) {
            return new ParteResponse(
                    pagamento.forma().name(), pagamento.valor().valor(),
                    pagamento.bandeira().map(Enum::name).orElse(null),
                    pagamento.codigoAutorizacao().orElse(null),
                    pagamento.maquininhaIntegrada(),
                    pagamento.valorRecebido().map(Dinheiro::valor).orElse(null),
                    pagamento.troco().map(Dinheiro::valor).orElse(null),
                    statusPresencial(pagamento.status()));
        }
    }

    public static VendaBalcaoResponse de(VendaBalcao venda, Map<String, String> nomesPorEmail) {
        if (!venda.presencial()) {
            return dePix(venda.pedido(), venda.cobrancaPix().orElseThrow());
        }
        return dePresencial(venda.pedido(), venda.pagamentos(), nomesPorEmail.get(venda.operador().orElseThrow()));
    }

    private static VendaBalcaoResponse dePresencial(Pedido pedido, List<PagamentoPresencial> pagamentos, String operadorNome) {
        PagamentoPresencial primeiro = pagamentos.getFirst();
        Optional<PagamentoPresencial> cartao = pagamentos.stream()
                .filter(pagamento -> pagamento.forma() != FormaPagamentoPresencial.DINHEIRO).reduce((anterior, ultimo) -> ultimo);
        return new VendaBalcaoResponse(
                pedido.id(), pedido.status().name(), itens(pedido), pedido.valorTotal().valor(), pedido.desconto().valor(),
                pedido.cpfNaNota().orElse(null),
                pagamentos.size() > 1 ? FORMA_DIVIDIDO : primeiro.forma().name(),
                cartao.flatMap(PagamentoPresencial::bandeira).map(Enum::name).orElse(null),
                cartao.flatMap(PagamentoPresencial::codigoAutorizacao).orElse(null),
                pagamentos.stream().anyMatch(PagamentoPresencial::maquininhaIntegrada),
                somar(pagamentos.stream().map(PagamentoPresencial::valorRecebido).toList()),
                somar(pagamentos.stream().map(PagamentoPresencial::troco).toList()),
                statusDoConjunto(pagamentos),
                primeiro.operador(),
                operadorNome,
                primeiro.criadoEm(),
                pagamentos.stream().map(ParteResponse::de).toList());
    }

    private static VendaBalcaoResponse dePix(Pedido pedido, Cobranca cobranca) {
        return new VendaBalcaoResponse(
                pedido.id(), pedido.status().name(), itens(pedido), pedido.valorTotal().valor(), pedido.desconto().valor(),
                pedido.cpfNaNota().orElse(null),
                FORMA_PIX_NA_TELA, null, null, false, null, null,
                statusDoPix(cobranca.status()),
                null,
                null,
                cobranca.criadoEm(),
                List.of());
    }

    /** Null quando nenhuma parte tem o valor (ex.: recebido só existe no dinheiro). */
    private static BigDecimal somar(List<Optional<Dinheiro>> valores) {
        List<Dinheiro> presentes = valores.stream().flatMap(Optional::stream).toList();
        if (presentes.isEmpty()) {
            return null;
        }
        return presentes.stream().reduce(Dinheiro.zero(), Dinheiro::somar).valor();
    }

    /** A venda só está paga quando todas as partes estão; a parte pendente manda no status. */
    private static String statusDoConjunto(List<PagamentoPresencial> pagamentos) {
        List<String> status = pagamentos.stream().map(pagamento -> statusPresencial(pagamento.status())).toList();
        if (status.contains(AGUARDANDO)) {
            return AGUARDANDO;
        }
        if (status.contains(RECUSADO)) {
            return RECUSADO;
        }
        return status.stream().allMatch(ESTORNADO::equals) ? ESTORNADO : APROVADO;
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

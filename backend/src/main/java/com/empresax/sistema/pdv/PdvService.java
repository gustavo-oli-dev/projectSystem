package com.empresax.sistema.pdv;

import com.empresax.sistema.cobranca.Cobranca;
import com.empresax.sistema.cobranca.CobrancaCriada;
import com.empresax.sistema.cobranca.CobrancaService;
import com.empresax.sistema.cobranca.MeioCobranca;
import com.empresax.sistema.common.domain.DomainException;
import com.empresax.sistema.common.domain.EntidadeNaoEncontradaException;
import com.empresax.sistema.documentofiscal.DocumentoFiscalService;
import com.empresax.sistema.pedido.Pedido;
import com.empresax.sistema.pdv.maquininha.Maquininha;
import com.empresax.sistema.pdv.maquininha.SituacaoCobrancaMaquininha;
import com.empresax.sistema.pedido.PedidoService;
import com.empresax.sistema.shared.dinheiro.Dinheiro;
import com.empresax.sistema.shared.documento.Cpf;
import com.empresax.sistema.venda.CancelamentoVendaService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Caixa (PDV). Toda venda tira os produtos do estoque na hora (estão no balcão, na mão do cliente).
 * - Dinheiro ou maquininha: pedido + estoque + pagamento + NFC-e numa transação só.
 * - Pix com QR na tela: cria a cobrança no Mercado Pago e espera; a NFC-e sai quando o Pix cair.
 *   Desistiu → cancelar devolve o estoque e cancela o Pix no Mercado Pago.
 */
@Service
public class PdvService {

    private static final String PREFIXO_IDEMPOTENCIA_ESTORNO = "estorno-pdv-";

    private final PedidoService pedidoService;
    private final PagamentoPresencialRepository pagamentoRepository;
    private final CobrancaService cobrancaService;
    private final DocumentoFiscalService documentoFiscalService;
    private final CancelamentoVendaService cancelamentoVendaService;
    private final Maquininha maquininha;

    public PdvService(
            PedidoService pedidoService,
            PagamentoPresencialRepository pagamentoRepository,
            CobrancaService cobrancaService,
            DocumentoFiscalService documentoFiscalService,
            CancelamentoVendaService cancelamentoVendaService,
            Maquininha maquininha
    ) {
        this.pedidoService = pedidoService;
        this.pagamentoRepository = pagamentoRepository;
        this.cobrancaService = cobrancaService;
        this.documentoFiscalService = documentoFiscalService;
        this.cancelamentoVendaService = cancelamentoVendaService;
        this.maquininha = maquininha;
    }

    @Transactional
    public VendaBalcao vender(DadosVendaBalcao venda, DadosPagamentoPresencial dadosPagamento, String operador) {
        Pedido pedido = criarEConfirmar(venda, operador);
        PagamentoPresencial pagamento = pagamentoRepository.save(
                montarPagamento(pedido.id(), pedido.valorTotal(), dadosPagamento, operador));
        documentoFiscalService.gerarPendentes(pedido.id());
        return VendaBalcao.presencial(pedido, pagamento);
    }

    /**
     * Maquininha integrada: separa o estoque e manda o valor para a maquininha. Se a maquininha não
     * responder, nada é gravado (transação única) — o operador pode usar a contingência.
     */
    @Transactional
    public VendaBalcao iniciarNaMaquininha(DadosVendaBalcao venda, FormaPagamentoPresencial forma, String operador) {
        Pedido pedido = criarEConfirmar(venda, operador);
        String idTransacao = maquininha.enviarCobranca(pedido.valorTotal(), forma, pedido.id());
        PagamentoPresencial pagamento = pagamentoRepository.save(
                PagamentoPresencial.aguardandoMaquininha(pedido.id(), forma, pedido.valorTotal(), idTransacao, operador));
        return VendaBalcao.presencial(pedido, pagamento);
    }

    /** O caixa pergunta a cada poucos segundos: aprovado gera a NFC-e; recusado permite tentar de novo. */
    @Transactional
    public VendaBalcao acompanharMaquininha(UUID pedidoId) {
        Pedido pedido = buscarDoBalcao(pedidoId);
        PagamentoPresencial pagamento = buscarPagamento(pedidoId);
        if (pagamento.aguardandoMaquininha()) {
            aplicarResultado(pagamento, maquininha.consultar(pagamento.idTransacaoMaquininha().orElseThrow()));
        }
        if (pagamento.aprovado() && documentoFiscalService.listarPorPedido(pedidoId).isEmpty()) {
            documentoFiscalService.gerarPendentes(pedidoId);
        }
        return VendaBalcao.presencial(pedido, pagamento);
    }

    @Transactional
    public VendaBalcao tentarDeNovoNaMaquininha(UUID pedidoId) {
        Pedido pedido = buscarDoBalcao(pedidoId);
        PagamentoPresencial pagamento = buscarPagamento(pedidoId);
        String idTransacao = maquininha.enviarCobranca(pagamento.valor(), pagamento.forma(), pedidoId);
        pagamento.novaTentativaNaMaquininha(idTransacao);
        return VendaBalcao.presencial(pedido, pagamento);
    }

    /** Falha no Mercado Pago desfaz tudo (inclusive a baixa no estoque): a transação é uma só. */
    @Transactional
    public CobrancaCriada iniciarVendaComPix(DadosVendaBalcao venda, String operador) {
        Pedido pedido = criarEConfirmar(venda, operador);
        return cobrancaService.criar(pedido.id(), MeioCobranca.PIX);
    }

    /**
     * O caixa pergunta "o Pix caiu?" a cada poucos segundos. Confere direto com o Mercado Pago (não
     * depende do aviso automático chegar) e, ao confirmar, gera a NFC-e.
     */
    @Transactional
    public VendaBalcao acompanharPix(UUID pedidoId) {
        Pedido pedido = buscarDoBalcao(pedidoId);
        Cobranca cobranca = cobrancaPixMaisRecente(pedidoId)
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Esta venda não tem Pix: " + pedidoId));
        cobrancaService.sincronizarComProvedor(cobranca);
        if (cobranca.foiPaga() && documentoFiscalService.listarPorPedido(pedidoId).isEmpty()) {
            documentoFiscalService.gerarPendentes(pedidoId);
        }
        return VendaBalcao.porPixNaTela(pedido, cobranca);
    }

    /**
     * Desfaz a venda e devolve ao estoque. Dinheiro/maquininha: estorna o pagamento (o estorno do
     * cartão é feito pelo operador na maquininha avulsa). Pix na tela pago: reembolso pelo Mercado
     * Pago. Pix ainda não pago: cancela o Pix no Mercado Pago.
     */
    @Transactional
    public VendaBalcao cancelar(UUID pedidoId, String operador) {
        buscarDoBalcao(pedidoId);
        Optional<PagamentoPresencial> presencial = pagamentoRepository.findByPedidoId(pedidoId);
        if (presencial.isPresent()) {
            desfazerPagamentoPresencial(presencial.get());
            return VendaBalcao.presencial(cancelamentoVendaService.cancelarVendaDoBalcao(pedidoId, operador), presencial.get());
        }
        Cobranca pix = cobrancaPixMaisRecente(pedidoId)
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Pagamento da venda não encontrado: " + pedidoId));
        Pedido cancelado = pix.foiPaga()
                ? cancelamentoVendaService.reembolsar(pedidoId, operador)
                : cancelamentoVendaService.cancelarVendaDoBalcao(pedidoId, operador);
        return VendaBalcao.porPixNaTela(cancelado, pix);
    }

    /**
     * Aguardando na maquininha: tira o valor da tela dela. Aprovado na maquininha integrada: estorna
     * pelo fornecedor. Dinheiro ou contingência: só registra (o operador devolve ou estorna na mão).
     */
    private void desfazerPagamentoPresencial(PagamentoPresencial pagamento) {
        if (pagamento.aguardandoMaquininha()) {
            maquininha.cancelarCobranca(pagamento.idTransacaoMaquininha().orElseThrow());
            pagamento.cancelarAntesDoPagamento();
            return;
        }
        if (!pagamento.aprovado()) {
            pagamento.cancelarAntesDoPagamento();
            return;
        }
        pagamento.idPagamentoProvedor().ifPresent(idPagamento ->
                maquininha.estornar(idPagamento, PREFIXO_IDEMPOTENCIA_ESTORNO + pagamento.id()));
        pagamento.estornar();
    }

    private static void aplicarResultado(PagamentoPresencial pagamento, SituacaoCobrancaMaquininha situacao) {
        switch (situacao.status()) {
            case APROVADA -> pagamento.confirmarPelaMaquininha(
                    situacao.bandeira(), situacao.codigoAutorizacao(), situacao.idPagamento());
            case RECUSADA, CANCELADA -> pagamento.recusarPelaMaquininha();
            case AGUARDANDO -> { /* cliente ainda não passou o cartão */ }
        }
    }

    private PagamentoPresencial buscarPagamento(UUID pedidoId) {
        return pagamentoRepository.findByPedidoId(pedidoId)
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Pagamento da venda não encontrado: " + pedidoId));
    }

    /** Uma venda do caixa, para o detalhe do pedido mostrar como foi paga. */
    @Transactional(readOnly = true)
    public VendaBalcao buscarVenda(UUID pedidoId) {
        Pedido pedido = buscarDoBalcao(pedidoId);
        return montarVenda(pedido, pagamentoRepository.findByPedidoId(pedidoId).orElse(null),
                cobrancaPixMaisRecente(pedidoId).orElse(null))
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Pagamento da venda não encontrado: " + pedidoId));
    }

    @Transactional(readOnly = true)
    public List<VendaBalcao> ultimasVendas() {
        List<Pedido> pedidos = pedidoService.listarUltimasDoBalcao();
        List<UUID> ids = pedidos.stream().map(Pedido::id).toList();
        Map<UUID, PagamentoPresencial> presenciais = pagamentoRepository.findByPedidoIdIn(ids).stream()
                .collect(Collectors.toMap(PagamentoPresencial::pedidoId, Function.identity()));
        Map<UUID, Cobranca> pixPorPedido = cobrancaService.listarPorPedidos(ids).stream()
                .filter(cobranca -> cobranca.meio() == MeioCobranca.PIX)
                .collect(Collectors.toMap(Cobranca::pedidoId, Function.identity(), PdvService::maisRecente));

        return pedidos.stream()
                .map(pedido -> montarVenda(pedido, presenciais.get(pedido.id()), pixPorPedido.get(pedido.id())))
                .flatMap(Optional::stream)
                .toList();
    }

    private Pedido criarEConfirmar(DadosVendaBalcao venda, String operador) {
        String cpfNaNota = venda.cpfNaNota();
        Cpf cpf = cpfNaNota == null || cpfNaNota.isBlank() ? null : new Cpf(cpfNaNota);
        Pedido pedido = pedidoService.criarNoBalcao(venda.itens(), cpf, venda.clienteId());
        pedidoService.confirmar(pedido.id(), operador);
        return pedido;
    }

    private Pedido buscarDoBalcao(UUID pedidoId) {
        Pedido pedido = pedidoService.buscarPorId(pedidoId);
        if (!pedido.vendidoNoBalcao()) {
            throw new DomainException("Esta venda não foi feita no balcão");
        }
        return pedido;
    }

    private Optional<Cobranca> cobrancaPixMaisRecente(UUID pedidoId) {
        return cobrancaService.listarPorPedido(pedidoId).stream()
                .filter(cobranca -> cobranca.meio() == MeioCobranca.PIX)
                .max(Comparator.comparing(Cobranca::criadoEm));
    }

    private static Cobranca maisRecente(Cobranca uma, Cobranca outra) {
        return uma.criadoEm().isAfter(outra.criadoEm()) ? uma : outra;
    }

    private static Optional<VendaBalcao> montarVenda(Pedido pedido, PagamentoPresencial presencial, Cobranca pix) {
        if (presencial != null) {
            return Optional.of(VendaBalcao.presencial(pedido, presencial));
        }
        return Optional.ofNullable(pix).map(cobranca -> VendaBalcao.porPixNaTela(pedido, cobranca));
    }

    private static PagamentoPresencial montarPagamento(
            UUID pedidoId, Dinheiro total, DadosPagamentoPresencial dados, String operador
    ) {
        if (dados == null || dados.forma() == null) {
            throw new DomainException("Escolha a forma de pagamento");
        }
        if (dados.forma() == FormaPagamentoPresencial.DINHEIRO) {
            Dinheiro recebido = dados.valorRecebido() == null ? null : new Dinheiro(dados.valorRecebido());
            return PagamentoPresencial.emDinheiro(pedidoId, total, recebido, operador);
        }
        return PagamentoPresencial.naMaquininhaAvulsa(
                pedidoId, dados.forma(), total, dados.bandeira(), dados.codigoAutorizacao(), operador);
    }
}

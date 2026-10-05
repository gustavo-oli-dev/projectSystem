package com.empresax.sistema.pdv;

import com.empresax.sistema.cobranca.Cobranca;
import com.empresax.sistema.cobranca.CobrancaCriada;
import com.empresax.sistema.cobranca.CobrancaService;
import com.empresax.sistema.cobranca.MeioCobranca;
import com.empresax.sistema.common.domain.DomainException;
import com.empresax.sistema.common.domain.EntidadeNaoEncontradaException;
import com.empresax.sistema.documentofiscal.DocumentoFiscalService;
import com.empresax.sistema.pedido.Pedido;
import com.empresax.sistema.pdv.autorizacao.AcaoAutorizada;
import com.empresax.sistema.pdv.autorizacao.AutorizacaoCaixaService;
import com.empresax.sistema.pdv.caixa.CaixaService;
import com.empresax.sistema.pdv.maquininha.Maquininha;
import com.empresax.sistema.pdv.maquininha.SituacaoCobrancaMaquininha;
import com.empresax.sistema.pedido.PedidoService;
import com.empresax.sistema.shared.dinheiro.Dinheiro;
import com.empresax.sistema.shared.documento.Cpf;
import com.empresax.sistema.venda.CancelamentoVendaService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
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
    private final CaixaService caixaService;
    private final AutorizacaoCaixaService autorizacaoService;

    public PdvService(
            PedidoService pedidoService,
            PagamentoPresencialRepository pagamentoRepository,
            CobrancaService cobrancaService,
            DocumentoFiscalService documentoFiscalService,
            CancelamentoVendaService cancelamentoVendaService,
            Maquininha maquininha,
            CaixaService caixaService,
            AutorizacaoCaixaService autorizacaoService
    ) {
        this.pedidoService = pedidoService;
        this.pagamentoRepository = pagamentoRepository;
        this.cobrancaService = cobrancaService;
        this.documentoFiscalService = documentoFiscalService;
        this.cancelamentoVendaService = cancelamentoVendaService;
        this.maquininha = maquininha;
        this.caixaService = caixaService;
        this.autorizacaoService = autorizacaoService;
    }

    /**
     * Venda paga na hora: as partes já recebidas (pagamento dividido, opcional) e a forma que fecha
     * a venda com o que falta (dinheiro com troco ou cartão/Pix em contingência).
     */
    @Transactional
    public VendaBalcao vender(DadosVendaBalcao venda, PartesDoPagamento partes, DadosPagamentoPresencial fechamento, String operador) {
        Pedido pedido = criarEConfirmar(venda, operador);
        List<PagamentoPresencial> pagamentos = new ArrayList<>(salvarPartes(pedido, partes, operador));
        pagamentos.add(pagamentoRepository.save(
                montarPagamento(pedido.id(), partes.restante(pedido.valorTotal()), fechamento, operador, false)));
        documentoFiscalService.gerarPendentes(pedido.id());
        return VendaBalcao.presencial(pedido, pagamentos);
    }

    /**
     * Maquininha integrada: separa o estoque, grava as partes já recebidas (se dividido) e manda o
     * que falta para a maquininha. Se a maquininha não responder, nada é gravado (transação única).
     */
    @Transactional
    public VendaBalcao iniciarNaMaquininha(DadosVendaBalcao venda, PartesDoPagamento partes, FormaPagamentoPresencial forma, String operador) {
        Pedido pedido = criarEConfirmar(venda, operador);
        List<PagamentoPresencial> pagamentos = new ArrayList<>(salvarPartes(pedido, partes, operador));
        Dinheiro restante = partes.restante(pedido.valorTotal());
        String idTransacao = maquininha.enviarCobranca(restante, forma, pedido.id());
        pagamentos.add(pagamentoRepository.save(
                PagamentoPresencial.aguardandoMaquininha(pedido.id(), forma, restante, idTransacao, operador)));
        return VendaBalcao.presencial(pedido, pagamentos);
    }

    /** O caixa pergunta a cada poucos segundos: aprovado gera a NFC-e; recusado permite tentar de novo. */
    @Transactional
    public VendaBalcao acompanharMaquininha(UUID pedidoId) {
        Pedido pedido = buscarDoBalcao(pedidoId);
        List<PagamentoPresencial> pagamentos = buscarPagamentos(pedidoId);
        pagamentos.stream().filter(PagamentoPresencial::aguardandoMaquininha).forEach(pagamento ->
                aplicarResultado(pagamento, maquininha.consultar(pagamento.idTransacaoMaquininha().orElseThrow())));
        boolean tudoAprovado = pagamentos.stream().allMatch(PagamentoPresencial::aprovado);
        if (tudoAprovado && documentoFiscalService.listarPorPedido(pedidoId).isEmpty()) {
            documentoFiscalService.gerarPendentes(pedidoId);
        }
        return VendaBalcao.presencial(pedido, pagamentos);
    }

    @Transactional
    public VendaBalcao tentarDeNovoNaMaquininha(UUID pedidoId) {
        Pedido pedido = buscarDoBalcao(pedidoId);
        List<PagamentoPresencial> pagamentos = buscarPagamentos(pedidoId);
        PagamentoPresencial recusado = pagamentos.stream().filter(PagamentoPresencial::recusado).findFirst()
                .orElseThrow(() -> new DomainException("Só dá para tentar de novo depois de uma recusa"));
        String idTransacao = maquininha.enviarCobranca(recusado.valor(), recusado.forma(), pedidoId);
        recusado.novaTentativaNaMaquininha(idTransacao);
        return VendaBalcao.presencial(pedido, pagamentos);
    }

    /** Partes já recebidas: dinheiro (valor exato ou com o recebido) ou cartão/Pix com o comprovante. */
    private List<PagamentoPresencial> salvarPartes(Pedido pedido, PartesDoPagamento partes, String operador) {
        partes.restante(pedido.valorTotal());
        return partes.partes().stream()
                .map(parte -> pagamentoRepository.save(
                        montarPagamento(pedido.id(), new Dinheiro(parte.valor()), parte, operador, true)))
                .toList();
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
        Pedido pedido = buscarDoBalcao(pedidoId);
        List<PagamentoPresencial> presenciais = buscarPagamentos(pedidoId);
        if (!presenciais.isEmpty()) {
            presenciais.forEach(pagamento -> {
                devolverDinheiroAoCliente(pedido, pagamento, operador);
                desfazerPagamentoPresencial(pagamento);
            });
            return VendaBalcao.presencial(cancelamentoVendaService.cancelarVendaDoBalcao(pedidoId, operador), presenciais);
        }
        Cobranca pix = cobrancaPixMaisRecente(pedidoId)
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Pagamento da venda não encontrado: " + pedidoId));
        Pedido cancelado = pix.foiPaga()
                ? cancelamentoVendaService.reembolsar(pedidoId, operador)
                : cancelamentoVendaService.cancelarVendaDoBalcao(pedidoId, operador);
        return VendaBalcao.porPixNaTela(cancelado, pix);
    }

    /** Dinheiro já recebido volta da gaveta para o cliente — o caixa precisa registrar isso. */
    private void devolverDinheiroAoCliente(Pedido pedido, PagamentoPresencial pagamento, String operador) {
        if (pagamento.forma() != FormaPagamentoPresencial.DINHEIRO || !pagamento.aprovado()) {
            return;
        }
        pedido.sessaoCaixaId().ifPresent(caixaDaVenda ->
                caixaService.registrarDevolucaoEmDinheiro(caixaDaVenda, pagamento.valor(), pedido.id(), operador));
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

    private List<PagamentoPresencial> buscarPagamentos(UUID pedidoId) {
        return pagamentoRepository.findByPedidoIdOrderByCriadoEmAsc(pedidoId);
    }

    /** Uma venda do caixa, para o detalhe do pedido mostrar como foi paga. */
    @Transactional(readOnly = true)
    public VendaBalcao buscarVenda(UUID pedidoId) {
        Pedido pedido = buscarDoBalcao(pedidoId);
        return montarVenda(pedido, buscarPagamentos(pedidoId), cobrancaPixMaisRecente(pedidoId).orElse(null))
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Pagamento da venda não encontrado: " + pedidoId));
    }

    @Transactional(readOnly = true)
    public List<VendaBalcao> ultimasVendas() {
        List<Pedido> pedidos = pedidoService.listarUltimasDoBalcao();
        List<UUID> ids = pedidos.stream().map(Pedido::id).toList();
        Map<UUID, List<PagamentoPresencial>> presenciais = pagamentoRepository.findByPedidoIdIn(ids).stream()
                .sorted(Comparator.comparing(PagamentoPresencial::criadoEm))
                .collect(Collectors.groupingBy(PagamentoPresencial::pedidoId));
        Map<UUID, Cobranca> pixPorPedido = cobrancaService.listarPorPedidos(ids).stream()
                .filter(cobranca -> cobranca.meio() == MeioCobranca.PIX)
                .collect(Collectors.toMap(Cobranca::pedidoId, Function.identity(), PdvService::maisRecente));

        return pedidos.stream()
                .map(pedido -> montarVenda(pedido, presenciais.getOrDefault(pedido.id(), List.of()), pixPorPedido.get(pedido.id())))
                .flatMap(Optional::stream)
                .toList();
    }

    private Pedido criarEConfirmar(DadosVendaBalcao venda, String operador) {
        String cpfNaNota = venda.cpfNaNota();
        Cpf cpf = cpfNaNota == null || cpfNaNota.isBlank() ? null : new Cpf(cpfNaNota);
        UUID caixa = caixaService.exigirCaixaAberto(operador).id();
        Pedido pedido = pedidoService.criarNoBalcao(venda.itens(), cpf, venda.clienteId(), caixa);
        if (venda.desconto() != null) {
            String autorizadoPor = autorizacaoService.validar(venda.desconto().tokenAutorizacao(), AcaoAutorizada.DESCONTO, operador);
            pedido.aplicarDesconto(new Dinheiro(venda.desconto().valor()), autorizadoPor);
        }
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

    private static Optional<VendaBalcao> montarVenda(Pedido pedido, List<PagamentoPresencial> presenciais, Cobranca pix) {
        if (!presenciais.isEmpty()) {
            return Optional.of(VendaBalcao.presencial(pedido, presenciais));
        }
        return Optional.ofNullable(pix).map(cobranca -> VendaBalcao.porPixNaTela(pedido, cobranca));
    }

    /**
     * @param parteDoDividido true = parte já recebida do pagamento dividido: em dinheiro, sem o valor
     *                        recebido, vale o valor exato da parte (sem troco).
     */
    private static PagamentoPresencial montarPagamento(
            UUID pedidoId, Dinheiro valor, DadosPagamentoPresencial dados, String operador, boolean parteDoDividido
    ) {
        if (dados == null || dados.forma() == null) {
            throw new DomainException("Escolha a forma de pagamento");
        }
        if (dados.forma() == FormaPagamentoPresencial.DINHEIRO) {
            Dinheiro recebido = dados.valorRecebido() != null ? new Dinheiro(dados.valorRecebido())
                    : parteDoDividido ? valor : null;
            return PagamentoPresencial.emDinheiro(pedidoId, valor, recebido, operador);
        }
        return PagamentoPresencial.naMaquininhaAvulsa(
                pedidoId, dados.forma(), valor, dados.bandeira(), dados.codigoAutorizacao(), operador);
    }
}

package com.empresax.sistema.pdv;

import com.empresax.sistema.cobranca.Cobranca;
import com.empresax.sistema.cobranca.CobrancaCriada;
import com.empresax.sistema.cobranca.CobrancaService;
import com.empresax.sistema.cobranca.MeioCobranca;
import com.empresax.sistema.common.domain.DomainException;
import com.empresax.sistema.common.domain.EntidadeNaoEncontradaException;
import com.empresax.sistema.documentofiscal.DocumentoFiscalService;
import com.empresax.sistema.pedido.ItemPedidoRequerido;
import com.empresax.sistema.pedido.Pedido;
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

    private final PedidoService pedidoService;
    private final PagamentoPresencialRepository pagamentoRepository;
    private final CobrancaService cobrancaService;
    private final DocumentoFiscalService documentoFiscalService;
    private final CancelamentoVendaService cancelamentoVendaService;

    public PdvService(
            PedidoService pedidoService,
            PagamentoPresencialRepository pagamentoRepository,
            CobrancaService cobrancaService,
            DocumentoFiscalService documentoFiscalService,
            CancelamentoVendaService cancelamentoVendaService
    ) {
        this.pedidoService = pedidoService;
        this.pagamentoRepository = pagamentoRepository;
        this.cobrancaService = cobrancaService;
        this.documentoFiscalService = documentoFiscalService;
        this.cancelamentoVendaService = cancelamentoVendaService;
    }

    @Transactional
    public VendaBalcao vender(
            List<ItemPedidoRequerido> itens, String cpfNaNota, DadosPagamentoPresencial dadosPagamento, String operador
    ) {
        Pedido pedido = criarEConfirmar(itens, cpfNaNota, operador);
        PagamentoPresencial pagamento = pagamentoRepository.save(
                montarPagamento(pedido.id(), pedido.valorTotal(), dadosPagamento, operador));
        documentoFiscalService.gerarPendentes(pedido.id());
        return VendaBalcao.presencial(pedido, pagamento);
    }

    /** Falha no Mercado Pago desfaz tudo (inclusive a baixa no estoque): a transação é uma só. */
    @Transactional
    public CobrancaCriada iniciarVendaComPix(List<ItemPedidoRequerido> itens, String cpfNaNota, String operador) {
        Pedido pedido = criarEConfirmar(itens, cpfNaNota, operador);
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
            presencial.get().estornar();
            return VendaBalcao.presencial(cancelamentoVendaService.cancelarVendaDoBalcao(pedidoId, operador), presencial.get());
        }
        Cobranca pix = cobrancaPixMaisRecente(pedidoId)
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Pagamento da venda não encontrado: " + pedidoId));
        Pedido cancelado = pix.foiPaga()
                ? cancelamentoVendaService.reembolsar(pedidoId, operador)
                : cancelamentoVendaService.cancelarVendaDoBalcao(pedidoId, operador);
        return VendaBalcao.porPixNaTela(cancelado, pix);
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

    private Pedido criarEConfirmar(List<ItemPedidoRequerido> itens, String cpfNaNota, String operador) {
        Cpf cpf = cpfNaNota == null || cpfNaNota.isBlank() ? null : new Cpf(cpfNaNota);
        Pedido pedido = pedidoService.criarNoBalcao(itens, cpf);
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

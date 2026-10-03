package com.empresax.sistema.pdv;

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

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Caixa (PDV). Uma venda de balcão acontece inteira numa transação: cria o pedido, confirma
 * (baixa o estoque), registra o pagamento e gera a NFC-e pendente. Faltou estoque ou o pagamento
 * é inválido → nada é gravado.
 */
@Service
public class PdvService {

    private final PedidoService pedidoService;
    private final PagamentoPresencialRepository pagamentoRepository;
    private final DocumentoFiscalService documentoFiscalService;
    private final CancelamentoVendaService cancelamentoVendaService;

    public PdvService(
            PedidoService pedidoService,
            PagamentoPresencialRepository pagamentoRepository,
            DocumentoFiscalService documentoFiscalService,
            CancelamentoVendaService cancelamentoVendaService
    ) {
        this.pedidoService = pedidoService;
        this.pagamentoRepository = pagamentoRepository;
        this.documentoFiscalService = documentoFiscalService;
        this.cancelamentoVendaService = cancelamentoVendaService;
    }

    @Transactional
    public VendaBalcao vender(
            List<ItemPedidoRequerido> itens, String cpfNaNota, DadosPagamentoPresencial dadosPagamento, String operador
    ) {
        Cpf cpf = cpfNaNota == null || cpfNaNota.isBlank() ? null : new Cpf(cpfNaNota);
        Pedido pedido = pedidoService.criarNoBalcao(itens, cpf);
        pedidoService.confirmar(pedido.id(), operador);

        PagamentoPresencial pagamento = pagamentoRepository.save(
                montarPagamento(pedido.id(), pedido.valorTotal(), dadosPagamento, operador));
        documentoFiscalService.gerarPendentes(pedido.id());
        return new VendaBalcao(pedido, pagamento);
    }

    /**
     * Desfaz a venda: estorna o pagamento, devolve ao estoque e descarta a NFC-e pendente. O estorno
     * do cartão na maquininha avulsa é feito pelo operador na própria maquininha.
     */
    @Transactional
    public VendaBalcao cancelar(UUID pedidoId, String operador) {
        Pedido pedido = pedidoService.buscarPorId(pedidoId);
        if (!pedido.vendidoNoBalcao()) {
            throw new DomainException("Esta venda não foi feita no balcão");
        }
        PagamentoPresencial pagamento = pagamentoRepository.findByPedidoId(pedidoId)
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Pagamento da venda não encontrado: " + pedidoId));
        pagamento.estornar();
        return new VendaBalcao(cancelamentoVendaService.cancelarVendaDoBalcao(pedidoId, operador), pagamento);
    }

    @Transactional(readOnly = true)
    public List<VendaBalcao> ultimasVendas() {
        List<Pedido> pedidos = pedidoService.listarUltimasDoBalcao();
        Map<UUID, PagamentoPresencial> pagamentos = pagamentoRepository
                .findByPedidoIdIn(pedidos.stream().map(Pedido::id).toList()).stream()
                .collect(Collectors.toMap(PagamentoPresencial::pedidoId, Function.identity()));
        return pedidos.stream()
                .filter(pedido -> pagamentos.containsKey(pedido.id()))
                .map(pedido -> new VendaBalcao(pedido, pagamentos.get(pedido.id())))
                .toList();
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

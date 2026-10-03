package com.empresax.sistema.venda;

import com.empresax.sistema.cobranca.Cobranca;
import com.empresax.sistema.cobranca.CobrancaRepository;
import com.empresax.sistema.cobranca.pagamento.ProvedorPagamento;
import com.empresax.sistema.common.domain.DomainException;
import com.empresax.sistema.pedido.ItemPedido;
import com.empresax.sistema.pedido.Pedido;
import com.empresax.sistema.pedido.PedidoService;
import com.empresax.sistema.produto.estoque.EstoqueService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Desfaz uma venda juntando as três partes: pedido, cobrança e estoque.
 * - Cancelar: pedido ainda não pago. Cobrança pendente é cancelada também no Mercado Pago.
 * - Reembolsar: pedido pago. Devolve o dinheiro pelo Mercado Pago e depois cancela.
 * Em ambos, o que tinha saído do estoque volta.
 *
 * O provedor é chamado antes de gravar: se o banco falhar depois, refazer a operação é seguro
 * porque o reembolso usa chave de idempotência (o cliente não recebe duas vezes).
 */
@Service
public class CancelamentoVendaService {

    private static final String PREFIXO_IDEMPOTENCIA_REEMBOLSO = "reembolso-";

    private final PedidoService pedidoService;
    private final CobrancaRepository cobrancaRepository;
    private final ProvedorPagamento provedorPagamento;
    private final EstoqueService estoqueService;

    public CancelamentoVendaService(
            PedidoService pedidoService,
            CobrancaRepository cobrancaRepository,
            ProvedorPagamento provedorPagamento,
            EstoqueService estoqueService
    ) {
        this.pedidoService = pedidoService;
        this.cobrancaRepository = cobrancaRepository;
        this.provedorPagamento = provedorPagamento;
        this.estoqueService = estoqueService;
    }

    @Transactional
    public Pedido cancelar(UUID pedidoId, String responsavel) {
        Pedido pedido = pedidoService.buscarPorId(pedidoId);
        List<Cobranca> cobrancas = cobrancaRepository.findByPedidoId(pedidoId);
        if (cobrancas.stream().anyMatch(Cobranca::foiPaga)) {
            throw new DomainException("Este pedido já foi pago. Use \"Reembolsar\" para devolver o dinheiro ao cliente.");
        }
        cancelarCobrancasPendentes(cobrancas);
        return cancelarDevolvendoEstoque(pedido, responsavel);
    }

    @Transactional
    public Pedido reembolsar(UUID pedidoId, String responsavel) {
        Pedido pedido = pedidoService.buscarPorId(pedidoId);
        List<Cobranca> cobrancas = cobrancaRepository.findByPedidoId(pedidoId);
        List<Cobranca> pagas = cobrancas.stream().filter(Cobranca::foiPaga).toList();
        if (pagas.isEmpty()) {
            throw new DomainException("Não há pagamento confirmado para reembolsar neste pedido");
        }
        for (Cobranca paga : pagas) {
            provedorPagamento.reembolsar(paga.referenciaExterna(), PREFIXO_IDEMPOTENCIA_REEMBOLSO + paga.id());
            paga.marcarComoReembolsada();
        }
        cancelarCobrancasPendentes(cobrancas);
        return cancelarDevolvendoEstoque(pedido, responsavel);
    }

    private void cancelarCobrancasPendentes(List<Cobranca> cobrancas) {
        for (Cobranca cobranca : cobrancas) {
            if (cobranca.aguardandoPagamento()) {
                provedorPagamento.cancelarCobranca(cobranca.referenciaExterna());
                cobranca.cancelar();
            }
        }
    }

    private Pedido cancelarDevolvendoEstoque(Pedido pedido, String responsavel) {
        boolean devolverEstoque = pedido.produtosSairamDoEstoque();
        pedido.cancelar();
        if (devolverEstoque) {
            for (ItemPedido item : pedido.itensDeProduto()) {
                estoqueService.devolverPorCancelamento(item.referenciaId(), item.quantidade(), pedido.id(), responsavel);
            }
        }
        return pedido;
    }
}

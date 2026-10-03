package com.empresax.sistema.cobranca;

import com.empresax.sistema.cliente.Cliente;
import com.empresax.sistema.cliente.ClienteService;
import com.empresax.sistema.cobranca.pagamento.DadosCobrancaExterna;
import com.empresax.sistema.cobranca.pagamento.ProvedorPagamento;
import com.empresax.sistema.common.domain.DomainException;
import com.empresax.sistema.common.domain.EntidadeNaoEncontradaException;
import com.empresax.sistema.pedido.Pedido;
import com.empresax.sistema.pedido.PedidoService;
import com.empresax.sistema.pedido.StatusPedido;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class CobrancaService {

    private final CobrancaRepository cobrancaRepository;
    private final PedidoService pedidoService;
    private final ClienteService clienteService;
    private final ProvedorPagamento provedorPagamento;

    public CobrancaService(
            CobrancaRepository cobrancaRepository,
            PedidoService pedidoService,
            ClienteService clienteService,
            ProvedorPagamento provedorPagamento
    ) {
        this.cobrancaRepository = cobrancaRepository;
        this.pedidoService = pedidoService;
        this.clienteService = clienteService;
        this.provedorPagamento = provedorPagamento;
    }

    @Transactional
    public CobrancaCriada criar(UUID pedidoId, MeioCobranca meio) {
        Pedido pedido = pedidoService.buscarPorId(pedidoId);
        garantirPedidoCobravel(pedido);
        Cliente cliente = clienteService.buscarPorId(pedido.clienteId());

        DadosCobrancaExterna dadosExternos = provedorPagamento.criarCobranca(
                pedido.valorTotal(), "Pedido " + pedido.id(), pedido.id(), meio, cliente);

        Cobranca cobranca = new Cobranca(pedido.id(), meio, pedido.valorTotal(), dadosExternos.referenciaExterna());
        cobrancaRepository.save(cobranca);
        return new CobrancaCriada(cobranca, dadosExternos);
    }

    private static void garantirPedidoCobravel(Pedido pedido) {
        if (pedido.status() != StatusPedido.AGUARDANDO_EMISSAO) {
            throw new DomainException("Só é possível cobrar um pedido confirmado e aguardando emissão fiscal");
        }
    }

    @Transactional(readOnly = true)
    public List<Cobranca> listarTodas() {
        return cobrancaRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Cobranca> listarPorPedido(UUID pedidoId) {
        return cobrancaRepository.findByPedidoId(pedidoId);
    }

    @Transactional(readOnly = true)
    public Cobranca buscarPorId(UUID id) {
        return cobrancaRepository.findById(id)
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Cobrança não encontrada: " + id));
    }
}

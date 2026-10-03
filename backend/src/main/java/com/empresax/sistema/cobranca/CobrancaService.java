package com.empresax.sistema.cobranca;

import com.empresax.sistema.cliente.ClienteService;
import com.empresax.sistema.cobranca.pagamento.DadosCobrancaExterna;
import com.empresax.sistema.cobranca.pagamento.ProvedorPagamento;
import com.empresax.sistema.common.domain.DomainException;
import com.empresax.sistema.common.domain.EntidadeNaoEncontradaException;
import com.empresax.sistema.pedido.Pedido;
import com.empresax.sistema.pedido.PedidoService;
import com.empresax.sistema.pedido.StatusPedido;
import com.empresax.sistema.shared.documento.Cpf;
import com.empresax.sistema.shared.documento.Documento;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
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
        Optional<Documento> documentoPagador = documentoDoPagador(pedido);
        if (meio == MeioCobranca.BOLETO && documentoPagador.isEmpty()) {
            throw new DomainException("Boleto exige CPF ou CNPJ do pagador");
        }

        DadosCobrancaExterna dadosExternos = provedorPagamento.criarCobranca(
                pedido.valorTotal(), "Pedido " + pedido.id(), pedido.id(), meio, documentoPagador);

        Cobranca cobranca = new Cobranca(pedido.id(), meio, pedido.valorTotal(), dadosExternos.referenciaExterna());
        cobrancaRepository.save(cobranca);
        return new CobrancaCriada(cobranca, dadosExternos);
    }

    /** Cliente cadastrado → documento dele; balcão → CPF na nota, se informado. */
    private Optional<Documento> documentoDoPagador(Pedido pedido) {
        Optional<Documento> doCliente = pedido.clienteId()
                .map(clienteId -> clienteService.buscarPorId(clienteId).documento());
        return doCliente.isPresent() ? doCliente : pedido.cpfNaNota().map(Cpf::new);
    }

    /**
     * Confere com o provedor uma cobrança ainda pendente (o caixa usa isso para saber se o Pix caiu,
     * sem depender do aviso automático do Mercado Pago chegar).
     */
    @Transactional
    public Cobranca sincronizarComProvedor(Cobranca cobranca) {
        if (cobranca.aguardandoPagamento()) {
            cobranca.aplicarStatusDoProvedor(provedorPagamento.consultarStatus(cobranca.referenciaExterna()));
            cobrancaRepository.save(cobranca);
        }
        return cobranca;
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
    public List<Cobranca> listarPorPedidos(List<UUID> pedidoIds) {
        return cobrancaRepository.findByPedidoIdIn(pedidoIds);
    }

    @Transactional(readOnly = true)
    public Cobranca buscarPorId(UUID id) {
        return cobrancaRepository.findById(id)
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Cobrança não encontrada: " + id));
    }
}

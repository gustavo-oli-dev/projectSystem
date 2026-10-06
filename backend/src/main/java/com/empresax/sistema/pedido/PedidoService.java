package com.empresax.sistema.pedido;

import com.empresax.sistema.cliente.ClienteService;
import com.empresax.sistema.common.domain.DomainException;
import com.empresax.sistema.common.domain.EntidadeNaoEncontradaException;
import com.empresax.sistema.produto.Produto;
import com.empresax.sistema.produto.ProdutoService;
import com.empresax.sistema.produto.estoque.EstoqueService;
import com.empresax.sistema.promocao.Promocao;
import com.empresax.sistema.promocao.PromocaoService;
import com.empresax.sistema.servico.Servico;
import com.empresax.sistema.servico.ServicoService;
import com.empresax.sistema.shared.dinheiro.Dinheiro;
import com.empresax.sistema.shared.documento.Cpf;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class PedidoService {

    private final PedidoRepository pedidoRepository;
    private final ClienteService clienteService;
    private final ProdutoService produtoService;
    private final ServicoService servicoService;
    private final EstoqueService estoqueService;
    private final PromocaoService promocaoService;

    public PedidoService(
            PedidoRepository pedidoRepository,
            ClienteService clienteService,
            ProdutoService produtoService,
            ServicoService servicoService,
            EstoqueService estoqueService,
            PromocaoService promocaoService
    ) {
        this.pedidoRepository = pedidoRepository;
        this.clienteService = clienteService;
        this.produtoService = produtoService;
        this.servicoService = servicoService;
        this.estoqueService = estoqueService;
        this.promocaoService = promocaoService;
    }

    @Transactional
    public Pedido criar(UUID clienteId, List<ItemPedidoRequerido> itensRequeridos) {
        clienteService.buscarPorId(clienteId);
        List<ItemPedido> itens = montarItens(itensRequeridos);
        Pedido pedido = new Pedido(clienteId, itens);
        return pedidoRepository.save(pedido);
    }

    /** Venda presencial: cliente cadastrado e CPF na nota são opcionais. Mesmas regras de estoque. */
    @Transactional
    public Pedido criarNoBalcao(
            List<ItemPedidoRequerido> itensRequeridos, Cpf cpfNaNota, UUID clienteId, UUID sessaoCaixaId
    ) {
        if (clienteId != null) {
            clienteService.buscarPorId(clienteId);
        }
        List<ItemPedido> itens = montarItens(itensRequeridos);
        return pedidoRepository.save(Pedido.noBalcao(itens, cpfNaNota, clienteId, sessaoCaixaId));
    }

    /** As promoções de hoje dos produtos pedidos vêm numa consulta só (sem N+1). */
    private List<ItemPedido> montarItens(List<ItemPedidoRequerido> itensRequeridos) {
        List<UUID> produtos = itensRequeridos.stream()
                .filter(requerido -> requerido.tipo() == TipoItem.PRODUTO)
                .map(ItemPedidoRequerido::referenciaId)
                .toList();
        Map<UUID, Promocao> promocoes = promocaoService.valendoHojePara(produtos);
        return itensRequeridos.stream().map(requerido -> montarItem(requerido, promocoes)).toList();
    }

    private ItemPedido montarItem(ItemPedidoRequerido requerido, Map<UUID, Promocao> promocoes) {
        return switch (requerido.tipo()) {
            case PRODUTO -> montarItemDeProduto(requerido, promocoes.get(requerido.referenciaId()));
            case SERVICO -> montarItemDeServico(requerido);
        };
    }

    /**
     * Já recusa aqui o que não tem estoque, para o cliente não pagar por algo que acabou. A baixa de
     * verdade (com trava) só acontece na confirmação.
     */
    private ItemPedido montarItemDeProduto(ItemPedidoRequerido requerido, Promocao promocaoDeHoje) {
        Produto produto = produtoService.buscarPorId(requerido.referenciaId());
        if (!produto.ativo()) {
            throw new DomainException("O produto \"" + produto.nome() + "\" não está à venda");
        }
        if (!produto.possuiEmEstoque(requerido.quantidade())) {
            throw new DomainException("Estoque insuficiente para \"" + produto.nome() + "\": disponível "
                    + produto.quantidadeEmEstoque());
        }
        ItemPedido item = new ItemPedido(TipoItem.PRODUTO, produto.id(), produto.nome(), produto.precoUnitario(),
                requerido.quantidade(), produto.custoUnitario().orElse(null));
        if (promocaoDeHoje != null) {
            Dinheiro desconto = promocaoDeHoje.descontoPara(produto.precoUnitario(), requerido.quantidade());
            if (desconto.valor().signum() > 0) {
                item.receberPromocao(desconto);
            }
        }
        return item;
    }

    private ItemPedido montarItemDeServico(ItemPedidoRequerido requerido) {
        Servico servico = servicoService.buscarPorId(requerido.referenciaId());
        return new ItemPedido(
                TipoItem.SERVICO, servico.id(), servico.nome(), servico.precoUnitario(), requerido.quantidade());
    }

    @Transactional(readOnly = true)
    public List<Pedido> listarTodos() {
        return pedidoRepository.findAllByOrderByCriadoEmDesc();
    }

    @Transactional(readOnly = true)
    public List<Pedido> listarUltimasDoBalcao() {
        return pedidoRepository.findTop50ByCanalOrderByCriadoEmDesc(CanalVenda.BALCAO);
    }

    @Transactional(readOnly = true)
    public Pedido buscarPorId(UUID id) {
        return pedidoRepository.findComItensById(id)
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Pedido não encontrado: " + id));
    }

    /**
     * Confirmar a venda tira os produtos do estoque, na mesma transação: se faltar um item, nada é
     * confirmado e nada sai do estoque.
     */
    @Transactional
    public Pedido confirmar(UUID id, String responsavel) {
        Pedido pedido = buscarPorId(id);
        pedido.confirmar();
        for (ItemPedido item : pedido.itensDeProduto()) {
            estoqueService.baixarPorVenda(item.referenciaId(), item.quantidade(), pedido.id(), responsavel);
        }
        return pedido;
    }
}

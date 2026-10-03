package com.empresax.sistema.pedido;

import com.empresax.sistema.cliente.ClienteService;
import com.empresax.sistema.common.domain.DomainException;
import com.empresax.sistema.common.domain.EntidadeNaoEncontradaException;
import com.empresax.sistema.produto.Produto;
import com.empresax.sistema.produto.ProdutoService;
import com.empresax.sistema.produto.estoque.EstoqueService;
import com.empresax.sistema.servico.Servico;
import com.empresax.sistema.servico.ServicoService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class PedidoService {

    private final PedidoRepository pedidoRepository;
    private final ClienteService clienteService;
    private final ProdutoService produtoService;
    private final ServicoService servicoService;
    private final EstoqueService estoqueService;

    public PedidoService(
            PedidoRepository pedidoRepository,
            ClienteService clienteService,
            ProdutoService produtoService,
            ServicoService servicoService,
            EstoqueService estoqueService
    ) {
        this.pedidoRepository = pedidoRepository;
        this.clienteService = clienteService;
        this.produtoService = produtoService;
        this.servicoService = servicoService;
        this.estoqueService = estoqueService;
    }

    @Transactional
    public Pedido criar(UUID clienteId, List<ItemPedidoRequerido> itensRequeridos) {
        clienteService.buscarPorId(clienteId);
        List<ItemPedido> itens = itensRequeridos.stream().map(this::montarItem).toList();
        Pedido pedido = new Pedido(clienteId, itens);
        return pedidoRepository.save(pedido);
    }

    private ItemPedido montarItem(ItemPedidoRequerido requerido) {
        return switch (requerido.tipo()) {
            case PRODUTO -> montarItemDeProduto(requerido);
            case SERVICO -> montarItemDeServico(requerido);
        };
    }

    /**
     * Já recusa aqui o que não tem estoque, para o cliente não pagar por algo que acabou. A baixa de
     * verdade (com trava) só acontece na confirmação.
     */
    private ItemPedido montarItemDeProduto(ItemPedidoRequerido requerido) {
        Produto produto = produtoService.buscarPorId(requerido.referenciaId());
        if (!produto.ativo()) {
            throw new DomainException("O produto \"" + produto.nome() + "\" não está à venda");
        }
        if (!produto.possuiEmEstoque(requerido.quantidade())) {
            throw new DomainException("Estoque insuficiente para \"" + produto.nome() + "\": disponível "
                    + produto.quantidadeEmEstoque());
        }
        return new ItemPedido(
                TipoItem.PRODUTO, produto.id(), produto.nome(), produto.precoUnitario(), requerido.quantidade());
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

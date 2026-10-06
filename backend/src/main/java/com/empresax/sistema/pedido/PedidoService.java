package com.empresax.sistema.pedido;

import com.empresax.sistema.cliente.ClienteService;
import com.empresax.sistema.common.domain.DomainException;
import com.empresax.sistema.common.domain.EntidadeNaoEncontradaException;
import com.empresax.sistema.produto.Produto;
import com.empresax.sistema.produto.ProdutoService;
import com.empresax.sistema.produto.embalagem.Embalagem;
import com.empresax.sistema.produto.embalagem.EmbalagemService;
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
import java.util.Objects;
import java.util.UUID;

@Service
public class PedidoService {

    private final PedidoRepository pedidoRepository;
    private final ClienteService clienteService;
    private final ProdutoService produtoService;
    private final ServicoService servicoService;
    private final EstoqueService estoqueService;
    private final PromocaoService promocaoService;
    private final EmbalagemService embalagemService;

    public PedidoService(
            PedidoRepository pedidoRepository,
            ClienteService clienteService,
            ProdutoService produtoService,
            ServicoService servicoService,
            EstoqueService estoqueService,
            PromocaoService promocaoService,
            EmbalagemService embalagemService
    ) {
        this.pedidoRepository = pedidoRepository;
        this.clienteService = clienteService;
        this.produtoService = produtoService;
        this.servicoService = servicoService;
        this.estoqueService = estoqueService;
        this.promocaoService = promocaoService;
        this.embalagemService = embalagemService;
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

    /** As promoções de hoje e as embalagens pedidas vêm numa consulta cada (sem N+1). */
    private List<ItemPedido> montarItens(List<ItemPedidoRequerido> itensRequeridos) {
        List<UUID> produtos = itensRequeridos.stream()
                .filter(requerido -> requerido.tipo() == TipoItem.PRODUTO)
                .map(ItemPedidoRequerido::referenciaId)
                .toList();
        List<UUID> embalagens = itensRequeridos.stream()
                .map(ItemPedidoRequerido::embalagemId)
                .filter(Objects::nonNull)
                .toList();
        CondicoesDaVenda condicoes = new CondicoesDaVenda(
                promocaoService.valendoHojePara(produtos), embalagemService.porIds(embalagens));
        return itensRequeridos.stream().map(requerido -> montarItem(requerido, condicoes)).toList();
    }

    /** Promoções de hoje e embalagens pedidas, buscadas uma vez para a venda inteira. */
    private record CondicoesDaVenda(Map<UUID, Promocao> promocoes, Map<UUID, Embalagem> embalagens) {
    }

    private ItemPedido montarItem(ItemPedidoRequerido requerido, CondicoesDaVenda condicoes) {
        return switch (requerido.tipo()) {
            case PRODUTO -> montarItemDeProduto(requerido, condicoes);
            case SERVICO -> montarItemDeServico(requerido);
        };
    }

    /**
     * Já recusa aqui o que não tem estoque, para o cliente não pagar por algo que acabou. A baixa de
     * verdade (com trava) só acontece na confirmação.
     */
    private ItemPedido montarItemDeProduto(ItemPedidoRequerido requerido, CondicoesDaVenda condicoes) {
        Produto produto = produtoService.buscarPorId(requerido.referenciaId());
        if (!produto.ativo()) {
            throw new DomainException("O produto \"" + produto.nome() + "\" não está à venda");
        }
        if (requerido.embalagemId() != null) {
            return montarItemDeEmbalagem(requerido, produto, condicoes.embalagens().get(requerido.embalagemId()));
        }
        garantirEstoque(produto, requerido.quantidade());
        ItemPedido item = new ItemPedido(TipoItem.PRODUTO, produto.id(), produto.nome(), produto.precoUnitario(),
                requerido.quantidade(), produto.custoUnitario().orElse(null));
        Promocao promocaoDeHoje = condicoes.promocoes().get(produto.id());
        if (promocaoDeHoje != null) {
            Dinheiro desconto = promocaoDeHoje.descontoPara(produto.precoUnitario(), requerido.quantidade());
            if (desconto.valor().signum() > 0) {
                item.receberPromocao(desconto);
            }
        }
        return item;
    }

    /** "Refrigerante — Fardo com 12": preço da embalagem; o estoque baixa em unidades (D41). */
    private ItemPedido montarItemDeEmbalagem(ItemPedidoRequerido requerido, Produto produto, Embalagem embalagem) {
        if (embalagem == null || !embalagem.ativa() || !embalagem.doProduto(produto.id())) {
            throw new DomainException("Esta embalagem de \"" + produto.nome() + "\" não está mais à venda");
        }
        garantirEstoque(produto, requerido.quantidade() * embalagem.unidades());
        ItemPedido item = new ItemPedido(TipoItem.PRODUTO, produto.id(), produto.nome() + " — " + embalagem.nome(),
                embalagem.preco(), requerido.quantidade(), produto.custoUnitario().map(embalagem::custoA).orElse(null));
        item.venderEmEmbalagem(embalagem.unidades());
        return item;
    }

    private static void garantirEstoque(Produto produto, int unidades) {
        if (!produto.possuiEmEstoque(unidades)) {
            throw new DomainException("Estoque insuficiente para \"" + produto.nome() + "\": disponível "
                    + produto.quantidadeEmEstoque());
        }
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
            estoqueService.baixarPorVenda(item.referenciaId(), item.unidadesDoEstoque(), pedido.id(), responsavel);
        }
        return pedido;
    }
}

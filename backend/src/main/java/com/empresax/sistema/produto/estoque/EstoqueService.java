package com.empresax.sistema.produto.estoque;

import com.empresax.sistema.common.domain.EntidadeNaoEncontradaException;
import com.empresax.sistema.produto.Produto;
import com.empresax.sistema.produto.ProdutoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Toda mudança de estoque passa por aqui: trava o produto, aplica a regra (na entidade) e registra
 * a movimentação. Venda e devolução exigem transação aberta por quem chama (confirmação ou
 * cancelamento do pedido), para o estoque e o pedido mudarem juntos ou não mudarem.
 */
@Service
public class EstoqueService {

    private final ProdutoRepository produtoRepository;
    private final MovimentacaoEstoqueRepository movimentacaoRepository;

    public EstoqueService(ProdutoRepository produtoRepository, MovimentacaoEstoqueRepository movimentacaoRepository) {
        this.produtoRepository = produtoRepository;
        this.movimentacaoRepository = movimentacaoRepository;
    }

    @Transactional
    public Produto darEntrada(UUID produtoId, int quantidade, String responsavel) {
        Produto produto = travar(produtoId);
        produto.darEntradaNoEstoque(quantidade);
        movimentacaoRepository.save(MovimentacaoEstoque.entrada(
                produto.id(), quantidade, produto.quantidadeEmEstoque(), responsavel));
        return produto;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void baixarPorVenda(UUID produtoId, int quantidade, UUID pedidoId, String responsavel) {
        Produto produto = travar(produtoId);
        produto.baixarDoEstoque(quantidade);
        movimentacaoRepository.save(MovimentacaoEstoque.venda(
                produto.id(), quantidade, produto.quantidadeEmEstoque(), pedidoId, responsavel));
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void devolverPorCancelamento(UUID produtoId, int quantidade, UUID pedidoId, String responsavel) {
        Produto produto = travar(produtoId);
        produto.devolverAoEstoque(quantidade);
        movimentacaoRepository.save(MovimentacaoEstoque.devolucao(
                produto.id(), quantidade, produto.quantidadeEmEstoque(), pedidoId, responsavel));
    }

    @Transactional(readOnly = true)
    public List<MovimentacaoEstoque> ultimasMovimentacoes(UUID produtoId) {
        return movimentacaoRepository.findTop50ByProdutoIdOrderByCriadaEmDesc(produtoId);
    }

    private Produto travar(UUID produtoId) {
        return produtoRepository.buscarParaAlterarEstoque(produtoId)
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Produto não encontrado: " + produtoId));
    }
}

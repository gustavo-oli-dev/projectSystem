package com.empresax.sistema.produto.estoque;

import com.empresax.sistema.common.domain.DomainException;
import com.empresax.sistema.common.domain.EntidadeNaoEncontradaException;
import com.empresax.sistema.produto.Produto;
import com.empresax.sistema.produto.ProdutoRepository;
import com.empresax.sistema.shared.dinheiro.Dinheiro;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
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

    /**
     * Entrada pela nota de compra: soma ao estoque e, se pedido, troca o custo do produto pelo da
     * nota. Roda dentro da transação da entrada da nota (estoque, nota e contas juntos ou nada).
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void darEntradaPorNota(UUID produtoId, int quantidade, UUID notaEntradaId, Dinheiro novoCusto, String responsavel) {
        Produto produto = travar(produtoId);
        produto.darEntradaNoEstoque(quantidade);
        if (novoCusto != null) {
            produto.definirCusto(novoCusto);
        }
        movimentacaoRepository.save(MovimentacaoEstoque.entradaPorNota(
                produto.id(), quantidade, produto.quantidadeEmEstoque(), notaEntradaId, responsavel));
    }

    /** Produto que sai sem ser vendido (vencido, avariado, furto...). Nunca deixa o estoque negativo. */
    @Transactional
    public Produto registrarPerda(UUID produtoId, int quantidade, MotivoPerda motivo, String observacao, String responsavel) {
        Produto produto = travar(produtoId);
        produto.baixarDoEstoque(quantidade);
        movimentacaoRepository.save(MovimentacaoEstoque.perda(
                produto.id(), quantidade, produto.quantidadeEmEstoque(), motivo, observacao,
                produto.custoUnitario().orElse(null), responsavel));
        return produto;
    }

    /**
     * Inventário: cada produto contado passa a ter o estoque contado; a diferença vira um acerto
     * registrado. Tudo numa transação — ou o inventário inteiro vale, ou nada muda.
     */
    @Transactional
    public List<AjusteInventario> aplicarInventario(Map<UUID, Integer> contagens, String responsavel) {
        if (contagens == null || contagens.isEmpty()) {
            throw new DomainException("Informe a contagem de ao menos um produto");
        }
        // Sempre na mesma ordem: dois inventários ao mesmo tempo não travam um ao outro.
        return contagens.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(contagem -> ajustar(contagem.getKey(), contagem.getValue(), responsavel))
                .toList();
    }

    private AjusteInventario ajustar(UUID produtoId, int quantidadeContada, String responsavel) {
        Produto produto = travar(produtoId);
        int noSistema = produto.quantidadeEmEstoque();
        int diferenca = produto.ajustarAoContado(quantidadeContada);
        if (diferenca != 0) {
            movimentacaoRepository.save(MovimentacaoEstoque.ajusteDeInventario(
                    produto.id(), diferenca, produto.quantidadeEmEstoque(), produto.custoUnitario().orElse(null), responsavel));
        }
        return new AjusteInventario(produto.id(), produto.nome(), noSistema, quantidadeContada, diferenca);
    }

    /** Resultado do inventário de um produto: o que o sistema tinha × o que foi contado. */
    public record AjusteInventario(UUID produtoId, String nome, int noSistema, int contado, int diferenca) {
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

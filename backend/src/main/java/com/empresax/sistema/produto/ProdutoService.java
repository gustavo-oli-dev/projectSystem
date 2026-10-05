package com.empresax.sistema.produto;

import com.empresax.sistema.common.domain.DomainException;
import com.empresax.sistema.common.domain.EntidadeNaoEncontradaException;
import com.empresax.sistema.shared.dinheiro.Dinheiro;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ProdutoService {

    private final ProdutoRepository produtoRepository;

    public ProdutoService(ProdutoRepository produtoRepository) {
        this.produtoRepository = produtoRepository;
    }

    @Transactional
    public Produto cadastrar(
            String nome, String descricao, String ncm, String unidadeMedida, Dinheiro precoUnitario, String codigoBarras,
            Dinheiro custoUnitario
    ) {
        Produto produto = new Produto(nome, descricao, ncm, unidadeMedida, precoUnitario, codigoBarras);
        produto.definirCusto(custoUnitario);
        garantirCodigoBarrasLivre(produto, null);
        return produtoRepository.save(produto);
    }

    @Transactional(readOnly = true)
    public List<Produto> listarTodos() {
        return produtoRepository.findAllByOrderByNomeAsc();
    }

    @Transactional(readOnly = true)
    public List<Produto> listarAtivos() {
        return produtoRepository.findByAtivoTrue();
    }

    @Transactional(readOnly = true)
    public Produto buscarPorId(UUID id) {
        return produtoRepository.findById(id)
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Produto não encontrado: " + id));
    }

    /** Produto do código de barras, se houver (ex.: sugerir o produto de um item da nota do fornecedor). */
    @Transactional(readOnly = true)
    public Optional<Produto> encontrarPorCodigoBarras(String codigoBarras) {
        return codigoBarras == null || codigoBarras.isBlank() ? Optional.empty() : produtoRepository.findByCodigoBarras(codigoBarras.trim());
    }

    /** Leitura pelo leitor de código de barras (caixa e entrada de estoque). */
    @Transactional(readOnly = true)
    public Produto buscarPorCodigoBarras(String codigoBarras) {
        return produtoRepository.findByCodigoBarras(codigoBarras.trim())
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Nenhum produto com o código " + codigoBarras.trim()));
    }

    @Transactional
    public Produto atualizar(
            UUID id, String nome, String descricao, Dinheiro precoUnitario, String codigoBarras, Dinheiro custoUnitario,
            Integer estoqueMinimo
    ) {
        Produto produto = buscarPorId(id);
        produto.atualizar(nome, descricao, precoUnitario, codigoBarras);
        produto.definirCusto(custoUnitario);
        produto.definirEstoqueMinimo(estoqueMinimo);
        garantirCodigoBarrasLivre(produto, id);
        return produto;
    }

    @Transactional
    public void desativar(UUID id) {
        buscarPorId(id).desativar();
    }

    @Transactional
    public Produto ativar(UUID id) {
        Produto produto = buscarPorId(id);
        produto.ativar();
        return produto;
    }

    /** Mensagem clara em vez de erro de banco quando dois produtos tentam usar o mesmo código. */
    private void garantirCodigoBarrasLivre(Produto produto, UUID idProprio) {
        produto.codigoBarras().ifPresent(codigo -> {
            boolean emUso = idProprio == null
                    ? produtoRepository.existsByCodigoBarras(codigo)
                    : produtoRepository.existsByCodigoBarrasAndIdNot(codigo, idProprio);
            if (emUso) {
                throw new DomainException("Já existe outro produto com o código de barras " + codigo);
            }
        });
    }
}

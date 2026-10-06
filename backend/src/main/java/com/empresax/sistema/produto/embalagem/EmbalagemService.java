package com.empresax.sistema.produto.embalagem;

import com.empresax.sistema.common.domain.DomainException;
import com.empresax.sistema.common.domain.EntidadeNaoEncontradaException;
import com.empresax.sistema.produto.ProdutoRepository;
import com.empresax.sistema.shared.dinheiro.Dinheiro;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Embalagens dos produtos (D41): cadastrar, remover e achar as de vários produtos de uma vez. */
@Service
public class EmbalagemService {

    private final EmbalagemRepository embalagemRepository;
    private final ProdutoRepository produtoRepository;

    public EmbalagemService(EmbalagemRepository embalagemRepository, ProdutoRepository produtoRepository) {
        this.embalagemRepository = embalagemRepository;
        this.produtoRepository = produtoRepository;
    }

    @Transactional
    public Embalagem adicionar(UUID produtoId, String nome, String codigoBarras, int unidades, Dinheiro preco) {
        if (!produtoRepository.existsById(produtoId)) {
            throw new EntidadeNaoEncontradaException("Produto não encontrado: " + produtoId);
        }
        Embalagem embalagem = new Embalagem(produtoId, nome, codigoBarras, unidades, preco);
        embalagem.codigoBarras().ifPresent(this::garantirCodigoLivre);
        return embalagemRepository.save(embalagem);
    }

    /** O leitor do caixa acha o produto ou a embalagem pelo código: os dois não podem repetir. */
    private void garantirCodigoLivre(String codigo) {
        if (produtoRepository.existsByCodigoBarras(codigo) || embalagemRepository.existsByCodigoBarras(codigo)) {
            throw new DomainException("O código de barras " + codigo + " já está em uso em outro produto ou embalagem");
        }
    }

    @Transactional
    public void remover(UUID produtoId, UUID embalagemId) {
        Embalagem embalagem = embalagemRepository.findById(embalagemId)
                .filter(encontrada -> encontrada.doProduto(produtoId))
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Embalagem não encontrada: " + embalagemId));
        embalagem.desativar();
    }

    /** Embalagens ativas por produto (lista de produtos, sem N+1). */
    @Transactional(readOnly = true)
    public Map<UUID, List<Embalagem>> ativasPorProduto() {
        return embalagemRepository.findByAtivaTrue().stream().collect(Collectors.groupingBy(Embalagem::produtoId));
    }

    @Transactional(readOnly = true)
    public List<Embalagem> ativasDo(UUID produtoId) {
        return embalagemRepository.findByProdutoIdAndAtivaTrue(produtoId);
    }

    /** As embalagens pedidas numa venda, por id (uma consulta). */
    @Transactional(readOnly = true)
    public Map<UUID, Embalagem> porIds(Collection<UUID> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        return embalagemRepository.findByIdIn(ids).stream().collect(Collectors.toMap(Embalagem::id, Function.identity()));
    }
}

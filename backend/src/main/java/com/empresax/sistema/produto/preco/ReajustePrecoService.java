package com.empresax.sistema.produto.preco;

import com.empresax.sistema.common.domain.DomainException;
import com.empresax.sistema.produto.Produto;
import com.empresax.sistema.produto.ProdutoRepository;
import com.empresax.sistema.shared.dinheiro.Dinheiro;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Preço em lote (D39): reajusta vários produtos de uma vez (percentual ou preço único). Tudo numa
 * transação — ou todos mudam, ou nenhum. Cada mudança fica no histórico (base das etiquetas).
 */
@Service
public class ReajustePrecoService {

    public static final int MAXIMO_DE_PRODUTOS = 500;

    private final ProdutoRepository produtoRepository;
    private final AlteracaoPrecoRepository alteracaoPrecoRepository;

    public ReajustePrecoService(ProdutoRepository produtoRepository, AlteracaoPrecoRepository alteracaoPrecoRepository) {
        this.produtoRepository = produtoRepository;
        this.alteracaoPrecoRepository = alteracaoPrecoRepository;
    }

    public record PrecoAlterado(UUID produtoId, String produto, Dinheiro precoAnterior, Dinheiro precoNovo) {
    }

    /** Devolve só os que mudaram de verdade (preço igual ao atual não gera registro). */
    @Transactional
    public List<PrecoAlterado> reajustar(Collection<UUID> produtoIds, ModoReajuste modo, BigDecimal valor, String quem) {
        Set<UUID> distintos = new LinkedHashSet<>(produtoIds == null ? List.of() : produtoIds);
        if (distintos.isEmpty()) {
            throw new DomainException("Escolha ao menos um produto");
        }
        if (distintos.size() > MAXIMO_DE_PRODUTOS) {
            throw new DomainException("Reajuste no máximo " + MAXIMO_DE_PRODUTOS + " produtos de uma vez");
        }
        if (modo == null || valor == null) {
            throw new DomainException("Informe como reajustar e o valor");
        }
        List<Produto> produtos = produtoRepository.findAllById(distintos);
        if (produtos.size() != distintos.size()) {
            throw new DomainException("Algum produto escolhido não existe mais: recarregue a lista");
        }
        List<PrecoAlterado> alterados = new ArrayList<>();
        for (Produto produto : produtos) {
            Dinheiro anterior = produto.precoUnitario();
            Dinheiro novo = modo.novoPreco(anterior, valor);
            if (novo.equals(anterior)) {
                continue;
            }
            produto.alterarPreco(novo);
            alteracaoPrecoRepository.save(new AlteracaoPreco(produto.id(), anterior, novo, quem));
            alterados.add(new PrecoAlterado(produto.id(), produto.nome(), anterior, novo));
        }
        return alterados;
    }

    /** Produtos que mudaram de preço desde a data — para reimprimir as etiquetas da gôndola. */
    @Transactional(readOnly = true)
    public Set<UUID> alteradosDesde(Instant desde) {
        Set<UUID> produtos = new LinkedHashSet<>();
        alteracaoPrecoRepository.findByAlteradoEmGreaterThanEqualOrderByAlteradoEmDesc(desde)
                .forEach(alteracao -> produtos.add(alteracao.produtoId()));
        return produtos;
    }
}

package com.empresax.sistema.produto.estoque;

import com.empresax.sistema.produto.Produto;
import com.empresax.sistema.produto.ProdutoRepository;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Sugestão de compra: produtos ativos que chegaram no estoque mínimo, com quanto comprar. */
@Service
public class ReposicaoService {

    /** Unidades vendidas por produto (venda confirmada e não cancelada) desde :desde. */
    private static final String SQL_VENDIDOS = """
            SELECT i.referencia_id AS produto, SUM(i.quantidade) AS unidades
            FROM itens_pedido i
            JOIN pedidos p ON p.id = i.pedido_id
            WHERE i.tipo = 'PRODUTO' AND p.status IN ('AGUARDANDO_EMISSAO', 'CONCLUIDO') AND p.confirmado_em >= :desde
            GROUP BY i.referencia_id
            """;

    private final ProdutoRepository produtoRepository;
    private final NamedParameterJdbcTemplate jdbc;

    public ReposicaoService(ProdutoRepository produtoRepository, NamedParameterJdbcTemplate jdbc) {
        this.produtoRepository = produtoRepository;
        this.jdbc = jdbc;
    }

    /** Os mais abaixo do mínimo primeiro. */
    @Transactional(readOnly = true)
    public List<SugestaoReposicao> sugerir() {
        Map<UUID, Integer> vendidos = vendidosNosUltimosDias();
        return produtoRepository.findAll().stream()
                .filter(Produto::ativo)
                .filter(Produto::precisaRepor)
                .map(produto -> SugestaoReposicao.para(produto, vendidos.getOrDefault(produto.id(), 0)))
                .sorted(Comparator.comparingInt((SugestaoReposicao sugestao) -> sugestao.estoque() - sugestao.estoqueMinimo())
                        .thenComparing(SugestaoReposicao::nome))
                .toList();
    }

    private Map<UUID, Integer> vendidosNosUltimosDias() {
        Instant desde = Instant.now().minus(Duration.ofDays(SugestaoReposicao.DIAS_DE_COBERTURA));
        Map<UUID, Integer> vendidos = new HashMap<>();
        jdbc.query(SQL_VENDIDOS, new MapSqlParameterSource("desde", Timestamp.from(desde)), (RowCallbackHandler) linha ->
                vendidos.put(linha.getObject("produto", UUID.class), linha.getInt("unidades")));
        return vendidos;
    }
}

package com.empresax.sistema.relatorios.estoque;

import com.empresax.sistema.relatorios.vendas.PeriodoRelatorio;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.List;

/** Relatório de perdas: perdas registradas + faltas achadas no inventário, no período. */
@Service
public class RelatorioPerdasService {

    /** Perda tem motivo; falta de inventário vira o motivo "INVENTARIO". */
    private static final String FILTRO = """
            FROM movimentacoes_estoque m
            JOIN produtos p ON p.id = m.produto_id
            WHERE m.tipo IN ('PERDA', 'INVENTARIO_FALTA') AND m.criada_em >= :comeco AND m.criada_em < :fim
            """;

    private static final String SQL_POR_MOTIVO = """
            SELECT COALESCE(m.motivo, '%s') AS chave, SUM(m.quantidade) AS unidades,
                   COALESCE(SUM(m.quantidade * m.custo_unitario), 0) AS valor
            %s
            GROUP BY COALESCE(m.motivo, '%s')
            ORDER BY valor DESC, unidades DESC
            """.formatted(RelatorioPerdas.MOTIVO_INVENTARIO, FILTRO, RelatorioPerdas.MOTIVO_INVENTARIO);

    private static final String SQL_POR_PRODUTO = """
            SELECT p.nome AS chave, SUM(m.quantidade) AS unidades, COALESCE(SUM(m.quantidade * m.custo_unitario), 0) AS valor
            %s
            GROUP BY p.nome
            ORDER BY valor DESC, unidades DESC
            LIMIT 20
            """.formatted(FILTRO);

    private static final String SQL_SEM_CUSTO = """
            SELECT COALESCE(SUM(m.quantidade), 0) %s AND m.custo_unitario IS NULL
            """.formatted(FILTRO);

    private final NamedParameterJdbcTemplate jdbc;

    public RelatorioPerdasService(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional(readOnly = true)
    public RelatorioPerdas gerar(PeriodoRelatorio periodo) {
        MapSqlParameterSource parametros = new MapSqlParameterSource()
                .addValue("comeco", Timestamp.from(periodo.comeco()))
                .addValue("fim", Timestamp.from(periodo.fimExclusivo()));
        List<RelatorioPerdas.PorMotivo> porMotivo = jdbc.query(SQL_POR_MOTIVO, parametros, (linha, indice) ->
                new RelatorioPerdas.PorMotivo(linha.getString("chave"), linha.getInt("unidades"), linha.getBigDecimal("valor")));
        List<RelatorioPerdas.PorProduto> porProduto = jdbc.query(SQL_POR_PRODUTO, parametros, (linha, indice) ->
                new RelatorioPerdas.PorProduto(linha.getString("chave"), linha.getInt("unidades"), linha.getBigDecimal("valor")));
        Integer semCusto = jdbc.queryForObject(SQL_SEM_CUSTO, parametros, Integer.class);
        return RelatorioPerdas.de(porMotivo, porProduto, semCusto == null ? 0 : semCusto);
    }
}

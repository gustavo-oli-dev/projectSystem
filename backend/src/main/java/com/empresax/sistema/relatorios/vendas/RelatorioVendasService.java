package com.empresax.sistema.relatorios.vendas;

import com.empresax.sistema.relatorios.vendas.RelatorioVendas.PontoSerie;
import com.empresax.sistema.relatorios.vendas.RelatorioVendas.ProdutoVendido;
import com.empresax.sistema.relatorios.vendas.RelatorioVendas.Recorte;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Relatório de vendas (D21). "Venda" = pedido confirmado e não cancelado (status AGUARDANDO_EMISSAO ou
 * CONCLUIDO), contado na data da confirmação, no fuso da loja. Agregação feita no banco, só com
 * consultas parametrizadas (nenhum SQL montado com dado de entrada).
 */
@Service
public class RelatorioVendasService {

    private static final int LIMITE_MAIS_VENDIDOS = 10;

    private static final String SQL_TOTAIS = """
            SELECT COUNT(DISTINCT p.id) AS vendas,
                   COALESCE(SUM(i.preco_unitario * i.quantidade - i.desconto), 0) AS faturamento,
                   COALESCE(SUM(i.quantidade) FILTER (WHERE i.tipo = 'PRODUTO'), 0) AS unidades,
                   COALESCE(SUM(i.custo_unitario * i.quantidade), 0) AS custo,
                   COALESCE(SUM(i.preco_unitario * i.quantidade - i.desconto) FILTER (WHERE i.custo_unitario IS NOT NULL), 0)
                       AS receita_com_custo
            FROM pedidos p
            JOIN itens_pedido i ON i.pedido_id = p.id
            WHERE p.status IN ('AGUARDANDO_EMISSAO', 'CONCLUIDO')
              AND p.confirmado_em >= :comeco AND p.confirmado_em < :fim
            """;

    private static final String SQL_SERIE = """
            SELECT CAST(date_trunc(:unidade, p.confirmado_em AT TIME ZONE :fuso) AS date) AS inicio,
                   COUNT(DISTINCT p.id) AS vendas,
                   COALESCE(SUM(i.preco_unitario * i.quantidade - i.desconto), 0) AS faturamento,
                   COALESCE(SUM(i.quantidade) FILTER (WHERE i.tipo = 'PRODUTO'), 0) AS unidades,
                   COALESCE(SUM(i.custo_unitario * i.quantidade), 0) AS custo,
                   COALESCE(SUM(i.preco_unitario * i.quantidade - i.desconto) FILTER (WHERE i.custo_unitario IS NOT NULL), 0)
                       AS receita_com_custo
            FROM pedidos p
            JOIN itens_pedido i ON i.pedido_id = p.id
            WHERE p.status IN ('AGUARDANDO_EMISSAO', 'CONCLUIDO')
              AND p.confirmado_em >= :comeco AND p.confirmado_em < :fim
            GROUP BY 1
            ORDER BY 1
            """;

    private static final String SQL_DESFEITAS = """
            SELECT 'DESFEITAS' AS chave, COUNT(DISTINCT p.id) AS vendas,
                   COALESCE(SUM(i.preco_unitario * i.quantidade - i.desconto), 0) AS valor
            FROM pedidos p
            JOIN itens_pedido i ON i.pedido_id = p.id
            WHERE p.status = 'CANCELADO'
              AND p.confirmado_em >= :comeco AND p.confirmado_em < :fim
            """;

    private static final String SQL_POR_CANAL = """
            SELECT p.canal AS chave, COUNT(DISTINCT p.id) AS vendas,
                   COALESCE(SUM(i.preco_unitario * i.quantidade - i.desconto), 0) AS valor
            FROM pedidos p
            JOIN itens_pedido i ON i.pedido_id = p.id
            WHERE p.status IN ('AGUARDANDO_EMISSAO', 'CONCLUIDO')
              AND p.confirmado_em >= :comeco AND p.confirmado_em < :fim
            GROUP BY p.canal
            """;

    /** Recebido no caixa (dinheiro, maquininha) e online (Pix com QR, Pix e boleto do Mercado Pago). */
    private static final String SQL_POR_FORMA = """
            SELECT pp.forma AS chave, COUNT(*) AS vendas, COALESCE(SUM(pp.valor), 0) AS valor
            FROM pagamentos_presenciais pp
            JOIN pedidos p ON p.id = pp.pedido_id
            WHERE pp.status = 'APROVADO'
              AND p.status IN ('AGUARDANDO_EMISSAO', 'CONCLUIDO')
              AND p.confirmado_em >= :comeco AND p.confirmado_em < :fim
            GROUP BY pp.forma
            UNION ALL
            SELECT CONCAT(c.meio, '_ONLINE') AS chave, COUNT(*) AS vendas, COALESCE(SUM(c.valor), 0) AS valor
            FROM cobrancas c
            JOIN pedidos p ON p.id = c.pedido_id
            WHERE c.status = 'PAGA'
              AND p.status IN ('AGUARDANDO_EMISSAO', 'CONCLUIDO')
              AND p.confirmado_em >= :comeco AND p.confirmado_em < :fim
            GROUP BY c.meio
            """;

    private static final String SQL_POR_HORA = """
            SELECT CAST(EXTRACT(HOUR FROM p.confirmado_em AT TIME ZONE :fuso) AS int) AS chave,
                   COUNT(DISTINCT p.id) AS vendas,
                   COALESCE(SUM(i.preco_unitario * i.quantidade - i.desconto), 0) AS valor
            FROM pedidos p
            JOIN itens_pedido i ON i.pedido_id = p.id
            WHERE p.status IN ('AGUARDANDO_EMISSAO', 'CONCLUIDO')
              AND p.confirmado_em >= :comeco AND p.confirmado_em < :fim
            GROUP BY 1
            """;

    /** ISODOW: 1 = segunda ... 7 = domingo. */
    private static final String SQL_POR_DIA_DA_SEMANA = """
            SELECT CAST(EXTRACT(ISODOW FROM p.confirmado_em AT TIME ZONE :fuso) AS int) AS chave,
                   COUNT(DISTINCT p.id) AS vendas,
                   COALESCE(SUM(i.preco_unitario * i.quantidade - i.desconto), 0) AS valor
            FROM pedidos p
            JOIN itens_pedido i ON i.pedido_id = p.id
            WHERE p.status IN ('AGUARDANDO_EMISSAO', 'CONCLUIDO')
              AND p.confirmado_em >= :comeco AND p.confirmado_em < :fim
            GROUP BY 1
            """;

    private static final String SQL_MAIS_VENDIDOS = """
            SELECT i.referencia_id AS id,
                   MAX(i.descricao) AS descricao,
                   MAX(i.tipo) AS tipo,
                   SUM(i.quantidade) AS unidades,
                   SUM(i.preco_unitario * i.quantidade - i.desconto) AS faturamento,
                   SUM((i.preco_unitario - i.custo_unitario) * i.quantidade - i.desconto) FILTER (WHERE i.custo_unitario IS NOT NULL) AS lucro,
                   BOOL_AND(i.custo_unitario IS NOT NULL) AS custo_completo
            FROM pedidos p
            JOIN itens_pedido i ON i.pedido_id = p.id
            WHERE p.status IN ('AGUARDANDO_EMISSAO', 'CONCLUIDO')
              AND p.confirmado_em >= :comeco AND p.confirmado_em < :fim
            GROUP BY i.referencia_id
            ORDER BY faturamento DESC
            LIMIT :limite
            """;

    private static final RowMapper<TotaisVendas> MAPEAR_TOTAIS = (linha, numero) -> new TotaisVendas(
            linha.getBigDecimal("faturamento"),
            linha.getLong("vendas"),
            linha.getLong("unidades"),
            linha.getBigDecimal("custo"),
            linha.getBigDecimal("receita_com_custo"));

    private static final RowMapper<Recorte> MAPEAR_RECORTE = (linha, numero) ->
            new Recorte(linha.getString("chave"), linha.getLong("vendas"), linha.getBigDecimal("valor"));

    private static final RowMapper<ProdutoVendido> MAPEAR_PRODUTO = (linha, numero) -> new ProdutoVendido(
            linha.getObject("id", UUID.class),
            linha.getString("descricao"),
            linha.getString("tipo"),
            linha.getLong("unidades"),
            linha.getBigDecimal("faturamento"),
            linha.getBigDecimal("lucro"),
            linha.getBoolean("custo_completo"));

    private final NamedParameterJdbcTemplate jdbc;

    public RelatorioVendasService(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional(readOnly = true)
    public RelatorioVendas gerar(PeriodoRelatorio periodo) {
        MapSqlParameterSource parametros = parametros(periodo);
        TotaisVendas totais = totais(parametros);
        List<Recorte> porForma = jdbc.query(SQL_POR_FORMA, parametros, MAPEAR_RECORTE);
        BigDecimal recebido = porForma.stream().map(Recorte::valor).reduce(BigDecimal.ZERO, BigDecimal::add);

        return new RelatorioVendas(
                periodo,
                periodo.granularidade(),
                totais,
                totais(parametros(periodo.anterior())),
                jdbc.queryForObject(SQL_DESFEITAS, parametros, MAPEAR_RECORTE),
                recebido,
                serieCompleta(periodo, parametros),
                jdbc.query(SQL_POR_CANAL, parametros, MAPEAR_RECORTE),
                porForma,
                jdbc.query(SQL_POR_HORA, parametros, MAPEAR_RECORTE),
                jdbc.query(SQL_POR_DIA_DA_SEMANA, parametros, MAPEAR_RECORTE),
                jdbc.query(SQL_MAIS_VENDIDOS, parametros.addValue("limite", LIMITE_MAIS_VENDIDOS), MAPEAR_PRODUTO));
    }

    private TotaisVendas totais(MapSqlParameterSource parametros) {
        return jdbc.queryForObject(SQL_TOTAIS, parametros, MAPEAR_TOTAIS);
    }

    /** Dias (ou semanas/meses) sem venda entram com zero — o gráfico não pode pular datas. */
    private List<PontoSerie> serieCompleta(PeriodoRelatorio periodo, MapSqlParameterSource parametros) {
        Granularidade granularidade = periodo.granularidade();
        MapSqlParameterSource comUnidade = new MapSqlParameterSource(parametros.getValues())
                .addValue("unidade", granularidade.unidadeSql());
        Map<LocalDate, TotaisVendas> vendidos = jdbc.query(SQL_SERIE, comUnidade, (linha, numero) -> Map.entry(
                        linha.getObject("inicio", LocalDate.class), MAPEAR_TOTAIS.mapRow(linha, numero)))
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        List<PontoSerie> serie = new ArrayList<>();
        for (LocalDate data = granularidade.inicioDoPeriodo(periodo.inicio());
             !data.isAfter(periodo.fim());
             data = granularidade.proximo(data)) {
            serie.add(new PontoSerie(data, vendidos.getOrDefault(data, TotaisVendas.zerado())));
        }
        return serie;
    }

    private static MapSqlParameterSource parametros(PeriodoRelatorio periodo) {
        return new MapSqlParameterSource()
                .addValue("comeco", Timestamp.from(periodo.comeco()))
                .addValue("fim", Timestamp.from(periodo.fimExclusivo()))
                .addValue("fuso", PeriodoRelatorio.FUSO_DA_LOJA.getId());
    }
}

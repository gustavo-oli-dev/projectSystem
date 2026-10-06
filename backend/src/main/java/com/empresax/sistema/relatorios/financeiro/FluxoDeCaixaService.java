package com.empresax.sistema.relatorios.financeiro;

import com.empresax.sistema.relatorios.vendas.PeriodoRelatorio;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.List;

/** Fluxo de caixa do Financeiro (D40). Agregado no banco, só com consultas parametrizadas. */
@Service
public class FluxoDeCaixaService {

    /** Mesmo critério do Painel: venda confirmada, pagamento aprovado ou cobrança paga; dia da confirmação. */
    private static final String SQL_ENTRADAS = """
            SELECT CAST(p.confirmado_em AT TIME ZONE :fuso AS date) AS dia, pp.forma AS forma, SUM(pp.valor) AS valor
            FROM pagamentos_presenciais pp
            JOIN pedidos p ON p.id = pp.pedido_id
            WHERE pp.status = 'APROVADO' AND p.status IN ('AGUARDANDO_EMISSAO', 'CONCLUIDO')
              AND p.confirmado_em >= :comeco AND p.confirmado_em < :fim
            GROUP BY 1, 2
            UNION ALL
            SELECT CAST(p.confirmado_em AT TIME ZONE :fuso AS date) AS dia, CONCAT(c.meio, '_ONLINE') AS forma, SUM(c.valor) AS valor
            FROM cobrancas c
            JOIN pedidos p ON p.id = c.pedido_id
            WHERE c.status = 'PAGA' AND p.status IN ('AGUARDANDO_EMISSAO', 'CONCLUIDO')
              AND p.confirmado_em >= :comeco AND p.confirmado_em < :fim
            GROUP BY 1, 2
            ORDER BY 1, 2
            """;

    private static final String SQL_SAIDAS = """
            SELECT CAST(c.paga_em AT TIME ZONE :fuso AS date) AS dia, c.descricao, ct.nome AS contato, c.valor
            FROM contas_a_pagar c
            LEFT JOIN contatos ct ON ct.id = c.contato_id
            WHERE c.status = 'PAGA' AND c.paga_em >= :comeco AND c.paga_em < :fim
            ORDER BY c.paga_em
            """;

    private static final String SQL_A_PAGAR = """
            SELECT COALESCE(SUM(valor), 0) FROM contas_a_pagar
            WHERE status = 'ABERTA' AND vencimento >= :inicio AND vencimento <= :ultimoDia
            """;

    private final NamedParameterJdbcTemplate jdbc;

    public FluxoDeCaixaService(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional(readOnly = true)
    public FluxoDeCaixa gerar(PeriodoRelatorio periodo) {
        MapSqlParameterSource parametros = new MapSqlParameterSource()
                .addValue("comeco", Timestamp.from(periodo.comeco()))
                .addValue("fim", Timestamp.from(periodo.fimExclusivo()))
                .addValue("inicio", Date.valueOf(periodo.inicio()))
                .addValue("ultimoDia", Date.valueOf(periodo.fim()))
                .addValue("fuso", PeriodoRelatorio.FUSO_DA_LOJA.getId());
        List<FluxoDeCaixa.Entrada> entradas = jdbc.query(SQL_ENTRADAS, parametros, (linha, indice) -> new FluxoDeCaixa.Entrada(
                linha.getDate("dia").toLocalDate(), linha.getString("forma"), linha.getBigDecimal("valor")));
        List<FluxoDeCaixa.Saida> saidas = jdbc.query(SQL_SAIDAS, parametros, (linha, indice) -> new FluxoDeCaixa.Saida(
                linha.getDate("dia").toLocalDate(), linha.getString("descricao"), linha.getString("contato"), linha.getBigDecimal("valor")));
        BigDecimal aPagar = jdbc.queryForObject(SQL_A_PAGAR, parametros, BigDecimal.class);
        return new FluxoDeCaixa(entradas, saidas, aPagar == null ? BigDecimal.ZERO : aPagar);
    }
}

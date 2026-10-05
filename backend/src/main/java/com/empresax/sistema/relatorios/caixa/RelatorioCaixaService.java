package com.empresax.sistema.relatorios.caixa;

import com.empresax.sistema.pdv.caixa.PontoCaixa;
import com.empresax.sistema.relatorios.vendas.PeriodoRelatorio;
import com.empresax.sistema.usuario.UsuarioService;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Relatório da gestão de caixa (D28): caixas abertos no período, pelo horário da abertura. */
@Service
public class RelatorioCaixaService {

    private static final String SQL_CAIXAS = """
            SELECT s.id, pc.numero AS ponto_numero, s.operador, s.aberta_por, s.fechada_por, s.aberta_em, s.fechada_em, s.fundo_inicial,
                   s.vendas_em_dinheiro, s.valor_esperado, s.valor_contado, s.observacao_fechamento,
                   COALESCE(SUM(m.valor) FILTER (WHERE m.tipo = 'SUPRIMENTO'), 0) AS reposicoes,
                   COALESCE(SUM(m.valor) FILTER (WHERE m.tipo = 'SANGRIA'), 0) AS sangrias
            FROM sessoes_caixa s
            LEFT JOIN pontos_caixa pc ON pc.id = s.ponto_caixa_id
            LEFT JOIN movimentos_caixa m ON m.sessao_caixa_id = s.id
            WHERE s.aberta_em >= :comeco AND s.aberta_em < :fim
            GROUP BY s.id, pc.numero
            ORDER BY s.aberta_em
            """;

    /** Vendido em cada forma, por caixa (pagamento aprovado; Pix por QR pago no Mercado Pago). */
    private static final String SQL_VENDAS_POR_FORMA = """
            SELECT p.sessao_caixa_id AS sessao, pp.forma AS forma, COALESCE(SUM(pp.valor), 0) AS valor
            FROM pagamentos_presenciais pp
            JOIN pedidos p ON p.id = pp.pedido_id
            JOIN sessoes_caixa s ON s.id = p.sessao_caixa_id
            WHERE s.aberta_em >= :comeco AND s.aberta_em < :fim AND pp.status = 'APROVADO'
            GROUP BY p.sessao_caixa_id, pp.forma
            UNION ALL
            SELECT p.sessao_caixa_id AS sessao, 'PIX_QR' AS forma, COALESCE(SUM(c.valor), 0) AS valor
            FROM cobrancas c
            JOIN pedidos p ON p.id = c.pedido_id
            JOIN sessoes_caixa s ON s.id = p.sessao_caixa_id
            WHERE s.aberta_em >= :comeco AND s.aberta_em < :fim AND c.meio = 'PIX' AND c.status = 'PAGA'
            GROUP BY p.sessao_caixa_id
            """;
    /**
     * Produtos vendidos em dinheiro nos caixas abertos no dia (venda cancelada sai: pagamento estornado).
     * Pagamento dividido (D36): o valor de cada produto entra na proporção paga em dinheiro.
     */
    private static final String SQL_PRODUTOS_EM_DINHEIRO = """
            WITH dinheiro_por_pedido AS (
                SELECT pp.pedido_id, SUM(pp.valor) AS em_dinheiro
                FROM pagamentos_presenciais pp
                WHERE pp.forma = 'DINHEIRO' AND pp.status = 'APROVADO'
                GROUP BY pp.pedido_id
            ), total_por_pedido AS (
                SELECT pedido_id, SUM(preco_unitario * quantidade - desconto) AS total
                FROM itens_pedido
                GROUP BY pedido_id
            )
            SELECT i.descricao AS descricao, SUM(i.quantidade) AS quantidade,
                   ROUND(SUM((i.preco_unitario * i.quantidade - i.desconto) * d.em_dinheiro / t.total), 2) AS valor
            FROM itens_pedido i
            JOIN pedidos p ON p.id = i.pedido_id
            JOIN dinheiro_por_pedido d ON d.pedido_id = p.id
            JOIN total_por_pedido t ON t.pedido_id = p.id
            JOIN sessoes_caixa s ON s.id = p.sessao_caixa_id
            WHERE s.aberta_em >= :comeco AND s.aberta_em < :fim
            GROUP BY i.descricao
            ORDER BY valor DESC, descricao
            """;
    private static final String SEM_CAIXA_NUMERADO = "Caixa sem número";

    private final NamedParameterJdbcTemplate jdbc;
    private final UsuarioService usuarioService;

    public RelatorioCaixaService(NamedParameterJdbcTemplate jdbc, UsuarioService usuarioService) {
        this.jdbc = jdbc;
        this.usuarioService = usuarioService;
    }

    /** Conferência do dinheiro de um dia: caixas abertos no dia + produtos vendidos em dinheiro neles. */
    @Transactional(readOnly = true)
    public DinheiroDoDia conferirDia(LocalDate dia) {
        PeriodoRelatorio periodo = new PeriodoRelatorio(dia, dia);
        List<DinheiroDoDia.ProdutoEmDinheiro> produtos = jdbc.query(SQL_PRODUTOS_EM_DINHEIRO, parametrosDo(periodo), (linha, indice) ->
                new DinheiroDoDia.ProdutoEmDinheiro(linha.getString("descricao"), linha.getInt("quantidade"), linha.getBigDecimal("valor")));
        return new DinheiroDoDia(dia, gerar(periodo).caixas(), produtos);
    }

    @Transactional(readOnly = true)
    public RelatorioCaixa gerar(PeriodoRelatorio periodo) {
        MapSqlParameterSource parametros = parametrosDo(periodo);
        List<LinhaCaixa> linhas = jdbc.query(SQL_CAIXAS, parametros, (resultado, indice) -> LinhaCaixa.ler(resultado));
        Map<UUID, Map<String, BigDecimal>> vendasPorCaixa = new HashMap<>();
        jdbc.query(SQL_VENDAS_POR_FORMA, parametros, (RowCallbackHandler) resultado -> vendasPorCaixa
                .computeIfAbsent(resultado.getObject("sessao", UUID.class), sessao -> new HashMap<>())
                .merge(resultado.getString("forma"), resultado.getBigDecimal("valor"), BigDecimal::add));

        Set<String> emails = linhas.stream()
                .flatMap(linha -> Stream.of(linha.operador(), linha.abertaPor(), linha.fechadaPor()))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<String, String> nomes = usuarioService.nomesPorEmail(emails);
        return RelatorioCaixa.de(linhas.stream()
                .map(linha -> linha.comNomes(nomes, vendasPorCaixa.getOrDefault(linha.id(), Map.of())))
                .toList());
    }

    private static MapSqlParameterSource parametrosDo(PeriodoRelatorio periodo) {
        return new MapSqlParameterSource()
                .addValue("comeco", Timestamp.from(periodo.comeco()))
                .addValue("fim", Timestamp.from(periodo.fimExclusivo()));
    }

    /** Linha crua do banco (e-mails); vira CaixaDoPeriodo com os nomes resolvidos em lote. */
    private record LinhaCaixa(
            UUID id, Integer pontoNumero, String operador, String abertaPor, String fechadaPor, Instant abertaEm, Instant fechadaEm,
            BigDecimal fundoInicial, BigDecimal reposicoes, BigDecimal sangrias, BigDecimal vendasEmDinheiro,
            BigDecimal valorEsperado, BigDecimal valorContado, String observacao
    ) {

        static LinhaCaixa ler(ResultSet resultado) throws SQLException {
            Timestamp fechadaEm = resultado.getTimestamp("fechada_em");
            return new LinhaCaixa(
                    resultado.getObject("id", UUID.class),
                    (Integer) resultado.getObject("ponto_numero"),
                    resultado.getString("operador"),
                    resultado.getString("aberta_por"),
                    resultado.getString("fechada_por"),
                    resultado.getTimestamp("aberta_em").toInstant(),
                    fechadaEm == null ? null : fechadaEm.toInstant(),
                    resultado.getBigDecimal("fundo_inicial"),
                    resultado.getBigDecimal("reposicoes"),
                    resultado.getBigDecimal("sangrias"),
                    resultado.getBigDecimal("vendas_em_dinheiro"),
                    resultado.getBigDecimal("valor_esperado"),
                    resultado.getBigDecimal("valor_contado"),
                    resultado.getString("observacao_fechamento"));
        }

        CaixaDoPeriodo comNomes(Map<String, String> nomes, Map<String, BigDecimal> vendasPorForma) {
            String pontoNome = pontoNumero == null ? SEM_CAIXA_NUMERADO : PontoCaixa.nomeDoNumero(pontoNumero);
            return new CaixaDoPeriodo(
                    id, pontoNome, operador, nomes.getOrDefault(operador, operador), nomes.getOrDefault(abertaPor, abertaPor),
                    fechadaPor == null ? null : nomes.getOrDefault(fechadaPor, fechadaPor),
                    abertaEm, fechadaEm, fundoInicial, reposicoes, sangrias, vendasEmDinheiro,
                    valorEsperado, valorContado, observacao, vendasPorForma);
        }
    }
}

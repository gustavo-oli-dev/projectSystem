package com.empresax.sistema.relatorios.caixa;

import com.empresax.sistema.relatorios.vendas.PeriodoRelatorio;
import com.empresax.sistema.usuario.UsuarioService;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
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
            SELECT s.id, s.operador, s.aberta_por, s.fechada_por, s.aberta_em, s.fechada_em, s.fundo_inicial,
                   s.vendas_em_dinheiro, s.valor_esperado, s.valor_contado, s.observacao_fechamento,
                   COALESCE(SUM(m.valor) FILTER (WHERE m.tipo = 'SUPRIMENTO'), 0) AS reposicoes,
                   COALESCE(SUM(m.valor) FILTER (WHERE m.tipo = 'SANGRIA'), 0) AS sangrias
            FROM sessoes_caixa s
            LEFT JOIN movimentos_caixa m ON m.sessao_caixa_id = s.id
            WHERE s.aberta_em >= :comeco AND s.aberta_em < :fim
            GROUP BY s.id
            ORDER BY s.aberta_em
            """;

    private final NamedParameterJdbcTemplate jdbc;
    private final UsuarioService usuarioService;

    public RelatorioCaixaService(NamedParameterJdbcTemplate jdbc, UsuarioService usuarioService) {
        this.jdbc = jdbc;
        this.usuarioService = usuarioService;
    }

    @Transactional(readOnly = true)
    public RelatorioCaixa gerar(PeriodoRelatorio periodo) {
        MapSqlParameterSource parametros = new MapSqlParameterSource()
                .addValue("comeco", Timestamp.from(periodo.comeco()))
                .addValue("fim", Timestamp.from(periodo.fimExclusivo()));
        List<LinhaCaixa> linhas = jdbc.query(SQL_CAIXAS, parametros, (resultado, indice) -> LinhaCaixa.ler(resultado));

        Set<String> emails = linhas.stream()
                .flatMap(linha -> Stream.of(linha.operador(), linha.abertaPor(), linha.fechadaPor()))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<String, String> nomes = usuarioService.nomesPorEmail(emails);
        return RelatorioCaixa.de(linhas.stream().map(linha -> linha.comNomes(nomes)).toList());
    }

    /** Linha crua do banco (e-mails); vira CaixaDoPeriodo com os nomes resolvidos em lote. */
    private record LinhaCaixa(
            UUID id, String operador, String abertaPor, String fechadaPor, Instant abertaEm, Instant fechadaEm,
            BigDecimal fundoInicial, BigDecimal reposicoes, BigDecimal sangrias, BigDecimal vendasEmDinheiro,
            BigDecimal valorEsperado, BigDecimal valorContado, String observacao
    ) {

        static LinhaCaixa ler(ResultSet resultado) throws SQLException {
            Timestamp fechadaEm = resultado.getTimestamp("fechada_em");
            return new LinhaCaixa(
                    resultado.getObject("id", UUID.class),
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

        CaixaDoPeriodo comNomes(Map<String, String> nomes) {
            return new CaixaDoPeriodo(
                    id, operador, nomes.getOrDefault(operador, operador), nomes.getOrDefault(abertaPor, abertaPor),
                    fechadaPor == null ? null : nomes.getOrDefault(fechadaPor, fechadaPor),
                    abertaEm, fechadaEm, fundoInicial, reposicoes, sangrias, vendasEmDinheiro,
                    valorEsperado, valorContado, observacao);
        }
    }
}

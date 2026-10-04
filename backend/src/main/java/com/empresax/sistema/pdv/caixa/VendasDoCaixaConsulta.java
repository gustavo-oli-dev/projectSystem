package com.empresax.sistema.pdv.caixa;

import com.empresax.sistema.shared.dinheiro.Dinheiro;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Soma das vendas de cada caixa. Só conta pagamento aprovado: venda cancelada tem o pagamento
 * estornado e sai da conta sozinha (o dinheiro voltou para o cliente).
 */
@Component
public class VendasDoCaixaConsulta {

    private static final String SQL_POR_FORMA = """
            SELECT pp.forma AS forma, COUNT(*) AS vendas, COALESCE(SUM(pp.valor), 0) AS valor
            FROM pagamentos_presenciais pp
            JOIN pedidos p ON p.id = pp.pedido_id
            WHERE p.sessao_caixa_id = :sessao AND pp.status = 'APROVADO'
            GROUP BY pp.forma
            UNION ALL
            SELECT 'PIX_QR' AS forma, COUNT(*) AS vendas, COALESCE(SUM(c.valor), 0) AS valor
            FROM cobrancas c
            JOIN pedidos p ON p.id = c.pedido_id
            WHERE p.sessao_caixa_id = :sessao AND c.meio = 'PIX' AND c.status = 'PAGA'
            HAVING COUNT(*) > 0
            """;

    private static final String SQL_DINHEIRO_POR_SESSAO = """
            SELECT p.sessao_caixa_id AS sessao, COALESCE(SUM(pp.valor), 0) AS valor
            FROM pagamentos_presenciais pp
            JOIN pedidos p ON p.id = pp.pedido_id
            WHERE p.sessao_caixa_id IN (:sessoes) AND pp.status = 'APROVADO' AND pp.forma = 'DINHEIRO'
            GROUP BY p.sessao_caixa_id
            """;

    private final NamedParameterJdbcTemplate jdbc;

    public VendasDoCaixaConsulta(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<VendasPorForma> porForma(UUID sessaoId) {
        return jdbc.query(SQL_POR_FORMA, new MapSqlParameterSource("sessao", sessaoId), (linha, indice) ->
                new VendasPorForma(linha.getString("forma"), linha.getInt("vendas"), new Dinheiro(linha.getBigDecimal("valor"))));
    }

    public Dinheiro emDinheiro(UUID sessaoId) {
        return emDinheiro(List.of(sessaoId)).getOrDefault(sessaoId, Dinheiro.zero());
    }

    /** Várias sessões numa consulta só (lista da conferência, sem N+1). */
    public Map<UUID, Dinheiro> emDinheiro(Collection<UUID> sessoes) {
        if (sessoes.isEmpty()) {
            return Map.of();
        }
        return jdbc.query(SQL_DINHEIRO_POR_SESSAO, new MapSqlParameterSource("sessoes", sessoes), (linha, indice) ->
                        Map.entry(linha.getObject("sessao", UUID.class), new Dinheiro(linha.getBigDecimal("valor"))))
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    /** forma: DINHEIRO, CARTAO_CREDITO, CARTAO_DEBITO, PIX (na maquininha) ou PIX_QR (QR na tela). */
    public record VendasPorForma(String forma, int vendas, Dinheiro valor) {
    }
}

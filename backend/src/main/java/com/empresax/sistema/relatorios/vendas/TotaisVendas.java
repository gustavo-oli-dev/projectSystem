package com.empresax.sistema.relatorios.vendas;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

/**
 * Somatório de vendas confirmadas num recorte (período, dia, canal...). O lucro só considera itens
 * com custo informado — por isso guardamos à parte a receita desses itens (receitaComCusto).
 */
public record TotaisVendas(
        BigDecimal faturamento,
        long vendas,
        long unidades,
        BigDecimal custo,
        BigDecimal receitaComCusto
) {

    private static final int CASAS_PERCENTUAL = 4;

    public static TotaisVendas zerado() {
        return new TotaisVendas(BigDecimal.ZERO, 0, 0, BigDecimal.ZERO, BigDecimal.ZERO);
    }

    public BigDecimal ticketMedio() {
        return vendas == 0 ? BigDecimal.ZERO : faturamento.divide(BigDecimal.valueOf(vendas), 2, RoundingMode.HALF_UP);
    }

    /** Receita dos itens com custo conhecido menos o custo deles. */
    public BigDecimal lucroBruto() {
        return receitaComCusto.subtract(custo);
    }

    /** Lucro ÷ receita com custo. Vazio quando nenhum item vendido tem custo informado. */
    public Optional<BigDecimal> margem() {
        if (receitaComCusto.signum() == 0) {
            return Optional.empty();
        }
        return Optional.of(lucroBruto().divide(receitaComCusto, CASAS_PERCENTUAL, RoundingMode.HALF_UP));
    }

    /** Quanto do faturamento tem custo informado (1 = lucro completo). */
    public BigDecimal coberturaDoCusto() {
        if (faturamento.signum() == 0) {
            return BigDecimal.ZERO;
        }
        return receitaComCusto.divide(faturamento, CASAS_PERCENTUAL, RoundingMode.HALF_UP);
    }
}

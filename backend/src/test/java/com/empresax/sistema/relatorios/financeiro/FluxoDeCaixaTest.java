package com.empresax.sistema.relatorios.financeiro;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FluxoDeCaixaTest {

    private static final LocalDate SEGUNDA = LocalDate.of(2026, 10, 5);
    private static final LocalDate TERCA = SEGUNDA.plusDays(1);

    @Test
    void saldoEhEntradasMenosSaidasNoTotalEDiaADia() {
        FluxoDeCaixa fluxo = new FluxoDeCaixa(
                List.of(new FluxoDeCaixa.Entrada(SEGUNDA, "DINHEIRO", valor("100.00")),
                        new FluxoDeCaixa.Entrada(SEGUNDA, "PIX", valor("50.00")),
                        new FluxoDeCaixa.Entrada(TERCA, "DINHEIRO", valor("30.00"))),
                List.of(new FluxoDeCaixa.Saida(TERCA, "Conta de luz", null, valor("80.00"))),
                valor("200.00"));

        assertThat(fluxo.totalEntradas()).isEqualByComparingTo("180.00");
        assertThat(fluxo.saldo()).isEqualByComparingTo("100.00");
        assertThat(fluxo.entradasPorForma().get("DINHEIRO")).isEqualByComparingTo("130.00");
        assertThat(fluxo.porDia()).extracting(FluxoDeCaixa.Dia::dia).containsExactly(SEGUNDA, TERCA);
        assertThat(fluxo.porDia().get(1).saldo()).isEqualByComparingTo("-50.00");
    }

    @Test
    void periodoSemMovimentoFicaZerado() {
        FluxoDeCaixa fluxo = new FluxoDeCaixa(List.of(), List.of(), BigDecimal.ZERO);

        assertThat(fluxo.saldo()).isEqualByComparingTo("0");
        assertThat(fluxo.porDia()).isEmpty();
    }

    @Test
    void exportacaoTemUmaLinhaPorEntradaEPorSaidaComSaidaNegativa() {
        FluxoDeCaixa fluxo = new FluxoDeCaixa(
                List.of(new FluxoDeCaixa.Entrada(SEGUNDA, "DINHEIRO", valor("10.00"))),
                List.of(new FluxoDeCaixa.Saida(SEGUNDA, "Frete", "Transportadora", valor("4.50"))),
                BigDecimal.ZERO);

        String csv = new String(ExportacaoFinanceiroCsv.lancamentos(fluxo), java.nio.charset.StandardCharsets.UTF_8);

        assertThat(csv).contains("05/10/2026;Entrada;Vendas em dinheiro;;10,00").contains("05/10/2026;Saída;Frete;Transportadora;-4,50");
    }

    private static BigDecimal valor(String texto) {
        return new BigDecimal(texto);
    }
}

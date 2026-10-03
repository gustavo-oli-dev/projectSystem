package com.empresax.sistema.relatorios.vendas;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class TotaisVendasTest {

    @Test
    void lucroEMargemConsideramSoOsItensComCustoInformado() {
        // Faturou 300; 200 disso tem custo de 120 → lucro 80, margem 40%, cobertura 2/3.
        TotaisVendas totais = new TotaisVendas(
                new BigDecimal("300.00"), 4, 10, new BigDecimal("120.00"), new BigDecimal("200.00"));

        assertThat(totais.lucroBruto()).isEqualByComparingTo("80.00");
        assertThat(totais.margem()).hasValueSatisfying(margem -> assertThat(margem).isEqualByComparingTo("0.4"));
        assertThat(totais.coberturaDoCusto()).isEqualByComparingTo("0.6667");
        assertThat(totais.ticketMedio()).isEqualByComparingTo("75.00");
    }

    @Test
    void semCustoInformadoNaoHaMargem() {
        TotaisVendas totais = new TotaisVendas(new BigDecimal("100.00"), 1, 1, BigDecimal.ZERO, BigDecimal.ZERO);

        assertThat(totais.margem()).isEmpty();
        assertThat(totais.coberturaDoCusto()).isEqualByComparingTo("0");
    }

    @Test
    void periodoSemVendasTemTicketZeroSemDividirPorZero() {
        assertThat(TotaisVendas.zerado().ticketMedio()).isEqualByComparingTo("0");
    }
}

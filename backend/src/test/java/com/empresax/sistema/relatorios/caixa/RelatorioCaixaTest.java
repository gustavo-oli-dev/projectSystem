package com.empresax.sistema.relatorios.caixa;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RelatorioCaixaTest {

    private static final Instant ABERTURA = Instant.parse("2026-10-04T11:00:00Z");
    private static final Instant FECHAMENTO = Instant.parse("2026-10-04T21:00:00Z");

    @Test
    void separaFaltasESobrasESomaOSaldoSoDosCaixasFechados() {
        RelatorioCaixa relatorio = RelatorioCaixa.de(List.of(
                fechado("ana@x.com", "Ana", "100.00", "90.00", "0", "0"),    // faltou 10
                fechado("ana@x.com", "Ana", "100.00", "105.00", "0", "20"),  // sobrou 5
                fechado("bia@x.com", "Bia", "200.00", "200.00", "50", "0"),  // bateu
                aberto("caio@x.com", "Caio")));

        assertThat(relatorio.totais().caixas()).isEqualTo(4);
        assertThat(relatorio.totais().fechados()).isEqualTo(3);
        assertThat(relatorio.totais().faltas()).isEqualByComparingTo("10.00");
        assertThat(relatorio.totais().sobras()).isEqualByComparingTo("5.00");
        assertThat(relatorio.totais().saldo()).isEqualByComparingTo("-5.00");
        assertThat(relatorio.totais().sangrias()).isEqualByComparingTo("20");
        assertThat(relatorio.totais().reposicoes()).isEqualByComparingTo("50");
        assertThat(relatorio.porResultado())
                .containsEntry(ResultadoFechamento.FALTOU, 1)
                .containsEntry(ResultadoFechamento.SOBROU, 1)
                .containsEntry(ResultadoFechamento.BATEU, 1)
                .containsEntry(ResultadoFechamento.ABERTO, 1);
    }

    @Test
    void porOperadorMostraPrimeiroQuemMaisFaltou() {
        RelatorioCaixa relatorio = RelatorioCaixa.de(List.of(
                fechado("bia@x.com", "Bia", "100.00", "100.00", "0", "0"),
                fechado("ana@x.com", "Ana", "100.00", "70.00", "0", "0"),
                fechado("ana@x.com", "Ana", "100.00", "95.00", "0", "0")));

        assertThat(relatorio.porOperador()).extracting(RelatorioCaixa.PorOperador::operadorNome).containsExactly("Ana", "Bia");
        RelatorioCaixa.PorOperador ana = relatorio.porOperador().get(0);
        assertThat(ana.caixas()).isEqualTo(2);
        assertThat(ana.comFalta()).isEqualTo(2);
        assertThat(ana.faltas()).isEqualByComparingTo("35.00");
    }

    @Test
    void periodoSemCaixasTotalizaZero() {
        RelatorioCaixa relatorio = RelatorioCaixa.de(List.of());

        assertThat(relatorio.totais().caixas()).isZero();
        assertThat(relatorio.totais().saldo()).isEqualByComparingTo("0");
        assertThat(relatorio.porOperador()).isEmpty();
    }

    @Test
    void csvTemUmaLinhaPorCaixaComDiferencaEmFormatoDoExcel() {
        RelatorioCaixa relatorio = RelatorioCaixa.de(List.of(
                fechado("ana@x.com", "Ana", "100.00", "80.20", "0", "0"), aberto("caio@x.com", "=Caio")));

        String csv = new String(ExportacaoCaixaCsv.fechamentos(relatorio), StandardCharsets.UTF_8);

        assertThat(csv.lines()).hasSize(3);
        assertThat(csv).contains("-19,80;Faltou");
        assertThat(csv).contains("'=Caio").contains("Ainda aberto");
    }

    private static CaixaDoPeriodo fechado(
            String email, String nome, String esperado, String contado, String reposicoes, String sangrias
    ) {
        return new CaixaDoPeriodo(UUID.randomUUID(), email, nome, "Gerente", "Gerente", ABERTURA, FECHAMENTO,
                new BigDecimal("90.00"), new BigDecimal(reposicoes), new BigDecimal(sangrias), new BigDecimal("10.00"),
                new BigDecimal(esperado), new BigDecimal(contado), null);
    }

    private static CaixaDoPeriodo aberto(String email, String nome) {
        return new CaixaDoPeriodo(UUID.randomUUID(), email, nome, "Gerente", null, ABERTURA, null,
                new BigDecimal("90.00"), BigDecimal.ZERO, BigDecimal.ZERO, null, null, null, null);
    }
}

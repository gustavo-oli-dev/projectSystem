package com.empresax.sistema.relatorios.caixa;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
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
    void resumoDoDiaSomaCadaFormaPorCaixaEListaQuemOperou() {
        Instant manha = Instant.parse("2026-10-04T12:00:00Z");
        Instant tarde = Instant.parse("2026-10-04T18:00:00Z");
        RelatorioCaixa relatorio = RelatorioCaixa.de(List.of(
                vendas("Caixa 01", "Ana", manha, Map.of("DINHEIRO", new BigDecimal("100"), "CARTAO_CREDITO", new BigDecimal("50"))),
                vendas("Caixa 01", "Bia", tarde, Map.of("DINHEIRO", new BigDecimal("20"), "PIX", new BigDecimal("30"))),
                vendas("Caixa 02", "Caio", manha, Map.of("CARTAO_DEBITO", new BigDecimal("70")))));

        assertThat(relatorio.porCaixaEDia()).hasSize(2);
        RelatorioCaixa.CaixaNoDia caixa01 = relatorio.porCaixaEDia().get(0);
        assertThat(caixa01.pontoNome()).isEqualTo("Caixa 01");
        assertThat(caixa01.operadores()).containsExactly("Ana", "Bia");
        assertThat(caixa01.vendasPorForma().get("DINHEIRO")).isEqualByComparingTo("120");
        assertThat(caixa01.totalVendido()).isEqualByComparingTo("200");
        assertThat(relatorio.porCaixaEDia().get(1).pontoNome()).isEqualTo("Caixa 02");
    }

    @Test
    void csvTemUmaLinhaPorCaixaComDiferencaEmFormatoDoExcel() {
        RelatorioCaixa relatorio = RelatorioCaixa.de(List.of(
                fechado("ana@x.com", "Ana", "100.00", "80.20", "0", "0"), aberto("caio@x.com", "=Caio")));

        String csv = new String(ExportacaoCaixaCsv.fechamentos(relatorio), StandardCharsets.UTF_8);

        assertThat(csv.lines()).hasSize(3);
        assertThat(csv).contains("-19,80;Devendo");
        assertThat(csv).contains("'=Caio").contains("Ainda aberto");
    }

    private static CaixaDoPeriodo fechado(
            String email, String nome, String esperado, String contado, String reposicoes, String sangrias
    ) {
        return new CaixaDoPeriodo(UUID.randomUUID(), "Caixa 01", email, nome, "Gerente", "Gerente", ABERTURA, FECHAMENTO,
                new BigDecimal("90.00"), new BigDecimal(reposicoes), new BigDecimal(sangrias), new BigDecimal("10.00"),
                new BigDecimal(esperado), new BigDecimal(contado), null, null, Map.of());
    }

    private static CaixaDoPeriodo vendas(String caixa, String operador, Instant abertura, Map<String, BigDecimal> porForma) {
        return new CaixaDoPeriodo(UUID.randomUUID(), caixa, operador + "@x.com", operador, "Gerente", null, abertura, null,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null, null, null, null, null, porForma);
    }

    private static CaixaDoPeriodo aberto(String email, String nome) {
        return new CaixaDoPeriodo(UUID.randomUUID(), "Caixa 02", email, nome, "Gerente", null, ABERTURA, null,
                new BigDecimal("90.00"), BigDecimal.ZERO, BigDecimal.ZERO, null, null, null, null, null, Map.of());
    }
}

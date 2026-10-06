package com.empresax.sistema.relatorios.caixa;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DinheiroDoDiaTest {

    private static final LocalDate DIA = LocalDate.of(2026, 10, 5);
    private static final Instant ABERTURA = Instant.parse("2026-10-05T11:00:00Z");
    private static final Instant FECHAMENTO = Instant.parse("2026-10-05T21:00:00Z");

    @Test
    void dinheiroQueEntrouIgualAoVendidoEmDinheiroDeixaODiaCerto() {
        // Inicial 50, vendeu 80 em dinheiro, repôs 20, sangrou 30: gaveta = 50 + 80 + 20 − 30 = 120.
        DinheiroDoDia dia = new DinheiroDoDia(DIA, List.of(fechado("Caixa 01", "50", "80", "20", "30", "120")), List.of());

        assertThat(dia.vendidoEmDinheiro()).isEqualByComparingTo("80");
        assertThat(dia.entrouNasGavetas()).isEqualByComparingTo("80");
        assertThat(dia.diferenca()).isEqualByComparingTo("0");
        assertThat(dia.resultado()).contains(ResultadoFechamento.BATEU);
    }

    @Test
    void somaTodosOsCaixasDoDiaEApontaQuandoODiaEstaDevendo() {
        DinheiroDoDia dia = new DinheiroDoDia(DIA, List.of(
                fechado("Caixa 01", "50", "100", "0", "0", "150"),   // certo
                fechado("Caixa 02", "50", "60", "0", "0", "100")),   // entrou 50, vendeu 60: devendo 10
                List.of());

        assertThat(dia.vendidoEmDinheiro()).isEqualByComparingTo("160");
        assertThat(dia.entrouNasGavetas()).isEqualByComparingTo("150");
        assertThat(dia.diferenca()).isEqualByComparingTo("-10");
        assertThat(dia.resultado()).contains(ResultadoFechamento.FALTOU);
    }

    @Test
    void caixaAindaAbertoFicaForaDaComparacaoEContadoAParte() {
        DinheiroDoDia dia = new DinheiroDoDia(DIA, List.of(fechado("Caixa 01", "50", "40", "0", "0", "90"), aberto()), List.of());

        assertThat(dia.caixasAbertos()).isEqualTo(1);
        assertThat(dia.vendidoEmDinheiro()).isEqualByComparingTo("40");
        assertThat(dia.resultado()).contains(ResultadoFechamento.BATEU);
    }

    @Test
    void diaSemCaixaFechadoNaoTemResultado() {
        assertThat(new DinheiroDoDia(DIA, List.of(aberto()), List.of()).resultado()).isEmpty();
    }

    private static CaixaDoPeriodo fechado(
            String caixa, String inicial, String vendidoEmDinheiro, String reposicoes, String sangrias, String contado
    ) {
        BigDecimal esperado = new BigDecimal(inicial).add(new BigDecimal(vendidoEmDinheiro))
                .add(new BigDecimal(reposicoes)).subtract(new BigDecimal(sangrias));
        return new CaixaDoPeriodo(UUID.randomUUID(), caixa, "ana@x.com", "Ana", "Gerente", "Gerente", ABERTURA, FECHAMENTO,
                new BigDecimal(inicial), new BigDecimal(reposicoes), new BigDecimal(sangrias), new BigDecimal(vendidoEmDinheiro),
                esperado, new BigDecimal(contado), null, null, Map.of("DINHEIRO", new BigDecimal(vendidoEmDinheiro)));
    }

    private static CaixaDoPeriodo aberto() {
        return new CaixaDoPeriodo(UUID.randomUUID(), "Caixa 03", "bia@x.com", "Bia", "Gerente", null, ABERTURA, null,
                new BigDecimal("50"), BigDecimal.ZERO, BigDecimal.ZERO, null, null, null, null, null, Map.of());
    }
}

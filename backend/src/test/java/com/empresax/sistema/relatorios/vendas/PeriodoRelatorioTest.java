package com.empresax.sistema.relatorios.vendas;

import com.empresax.sistema.common.domain.DomainException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PeriodoRelatorioTest {

    @Test
    void periodoAnteriorTemOMesmoTamanhoEAcabaNaVespera() {
        PeriodoRelatorio setembro = new PeriodoRelatorio(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30));

        PeriodoRelatorio anterior = setembro.anterior();

        assertThat(anterior.inicio()).isEqualTo(LocalDate.of(2026, 8, 2));
        assertThat(anterior.fim()).isEqualTo(LocalDate.of(2026, 8, 31));
        assertThat(anterior.dias()).isEqualTo(30);
    }

    @Test
    void agrupaPorDiaAteDoisMesesPorSemanaAteSeisEPorMesAcimaDisso() {
        LocalDate inicio = LocalDate.of(2026, 1, 1);

        assertThat(new PeriodoRelatorio(inicio, inicio.plusDays(29)).granularidade()).isEqualTo(Granularidade.DIA);
        assertThat(new PeriodoRelatorio(inicio, inicio.plusDays(89)).granularidade()).isEqualTo(Granularidade.SEMANA);
        assertThat(new PeriodoRelatorio(inicio, inicio.plusDays(364)).granularidade()).isEqualTo(Granularidade.MES);
    }

    @Test
    void diaDeHojeComecaEAcabaNoRelogioDeFortaleza() {
        PeriodoRelatorio hoje = new PeriodoRelatorio(LocalDate.of(2026, 10, 3), LocalDate.of(2026, 10, 3));

        assertThat(hoje.comeco().toString()).isEqualTo("2026-10-03T03:00:00Z");
        assertThat(hoje.fimExclusivo().toString()).isEqualTo("2026-10-04T03:00:00Z");
    }

    @Test
    void recusaFimAntesDoInicioEPeriodoGrandeDemais() {
        assertThatThrownBy(() -> new PeriodoRelatorio(LocalDate.of(2026, 9, 2), LocalDate.of(2026, 9, 1)))
                .isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> new PeriodoRelatorio(LocalDate.of(2020, 1, 1), LocalDate.of(2026, 1, 1)))
                .isInstanceOf(DomainException.class);
    }

    @Test
    void semanaComecaNaSegunda() {
        assertThat(Granularidade.SEMANA.inicioDoPeriodo(LocalDate.of(2026, 10, 3)))
                .isEqualTo(LocalDate.of(2026, 9, 28));
    }
}

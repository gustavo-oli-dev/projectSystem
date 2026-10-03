package com.empresax.sistema.relatorios.vendas;

import com.empresax.sistema.common.domain.DomainException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

/**
 * Período de um relatório, em dias do calendário da loja (Fortaleza). O fim é inclusivo para quem
 * escolhe as datas; internamente vira o intervalo [início 00:00, dia seguinte ao fim 00:00).
 */
public record PeriodoRelatorio(LocalDate inicio, LocalDate fim) {

    /** A loja fica em Fortaleza: "vendas de hoje" segue o relógio dela, não o do servidor. */
    public static final ZoneId FUSO_DA_LOJA = ZoneId.of("America/Fortaleza");
    private static final long DIAS_MAXIMOS = 731;
    private static final long LIMITE_DIARIO = 62;
    private static final long LIMITE_SEMANAL = 200;

    public PeriodoRelatorio {
        if (inicio == null || fim == null) {
            throw new DomainException("Informe o início e o fim do período");
        }
        if (fim.isBefore(inicio)) {
            throw new DomainException("O fim do período não pode ser antes do início");
        }
        if (ChronoUnit.DAYS.between(inicio, fim) >= DIAS_MAXIMOS) {
            throw new DomainException("O período do relatório pode ter no máximo 2 anos");
        }
    }

    public long dias() {
        return ChronoUnit.DAYS.between(inicio, fim) + 1;
    }

    /** Mesmo tamanho, imediatamente antes — base da comparação "vs período anterior". */
    public PeriodoRelatorio anterior() {
        return new PeriodoRelatorio(inicio.minusDays(dias()), inicio.minusDays(1));
    }

    /** Até 2 meses agrupa por dia; até ~6 meses por semana; acima disso por mês. */
    public Granularidade granularidade() {
        if (dias() <= LIMITE_DIARIO) {
            return Granularidade.DIA;
        }
        return dias() <= LIMITE_SEMANAL ? Granularidade.SEMANA : Granularidade.MES;
    }

    public Instant comeco() {
        return inicio.atStartOfDay(FUSO_DA_LOJA).toInstant();
    }

    public Instant fimExclusivo() {
        return fim.plusDays(1).atStartOfDay(FUSO_DA_LOJA).toInstant();
    }
}

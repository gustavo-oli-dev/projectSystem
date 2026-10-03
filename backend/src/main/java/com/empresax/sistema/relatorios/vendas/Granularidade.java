package com.empresax.sistema.relatorios.vendas;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

/** Tamanho de cada barra do gráfico de vendas. Cada valor sabe o seu nome no PostgreSQL e o próximo passo. */
public enum Granularidade {
    DIA("day") {
        @Override
        public LocalDate inicioDoPeriodo(LocalDate data) {
            return data;
        }

        @Override
        public LocalDate proximo(LocalDate data) {
            return data.plusDays(1);
        }
    },
    SEMANA("week") {
        @Override
        public LocalDate inicioDoPeriodo(LocalDate data) {
            return data.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        }

        @Override
        public LocalDate proximo(LocalDate data) {
            return data.plusWeeks(1);
        }
    },
    MES("month") {
        @Override
        public LocalDate inicioDoPeriodo(LocalDate data) {
            return data.withDayOfMonth(1);
        }

        @Override
        public LocalDate proximo(LocalDate data) {
            return data.plusMonths(1);
        }
    };

    private final String unidadeSql;

    Granularidade(String unidadeSql) {
        this.unidadeSql = unidadeSql;
    }

    /** Valor fixo do enum (nunca entrada do usuário) passado como parâmetro para date_trunc. */
    public String unidadeSql() {
        return unidadeSql;
    }

    public abstract LocalDate inicioDoPeriodo(LocalDate data);

    public abstract LocalDate proximo(LocalDate data);
}

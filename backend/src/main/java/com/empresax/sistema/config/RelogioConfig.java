package com.empresax.sistema.config;

import com.empresax.sistema.relatorios.vendas.PeriodoRelatorio;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/** "Hoje" é o dia em Fortaleza, não o do servidor (que roda em UTC). Injetável para os testes. */
@Configuration
public class RelogioConfig {

    @Bean
    public Clock relogioDaLoja() {
        return Clock.system(PeriodoRelatorio.FUSO_DA_LOJA);
    }
}

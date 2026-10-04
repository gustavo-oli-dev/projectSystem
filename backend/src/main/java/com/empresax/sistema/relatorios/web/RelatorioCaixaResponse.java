package com.empresax.sistema.relatorios.web;

import com.empresax.sistema.relatorios.caixa.CaixaDoPeriodo;
import com.empresax.sistema.relatorios.caixa.RelatorioCaixa;
import com.empresax.sistema.relatorios.caixa.ResultadoFechamento;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public record RelatorioCaixaResponse(
        RelatorioCaixa.Totais totais,
        List<QuantidadePorResultado> porResultado,
        List<RelatorioCaixa.PorOperador> porOperador,
        List<CaixaResponse> caixas
) {

    public record QuantidadePorResultado(ResultadoFechamento resultado, int caixas) {
    }

    public record CaixaResponse(
            UUID id,
            String operadorNome,
            String abertaPorNome,
            String fechadaPorNome,
            Instant abertaEm,
            Instant fechadaEm,
            BigDecimal fundoInicial,
            BigDecimal reposicoes,
            BigDecimal sangrias,
            BigDecimal vendasEmDinheiro,
            BigDecimal valorEsperado,
            BigDecimal valorContado,
            BigDecimal diferenca,
            ResultadoFechamento resultado
    ) {

        static CaixaResponse de(CaixaDoPeriodo caixa) {
            return new CaixaResponse(
                    caixa.id(), caixa.operadorNome(), caixa.abertaPorNome(), caixa.fechadaPorNome(),
                    caixa.abertaEm(), caixa.fechadaEm(), caixa.fundoInicial(), caixa.reposicoes(), caixa.sangrias(),
                    caixa.vendasEmDinheiro(), caixa.valorEsperado(), caixa.valorContado(),
                    caixa.diferenca().orElse(null), caixa.resultado());
        }
    }

    static RelatorioCaixaResponse de(RelatorioCaixa relatorio) {
        return new RelatorioCaixaResponse(
                relatorio.totais(),
                Arrays.stream(ResultadoFechamento.values())
                        .map(resultado -> new QuantidadePorResultado(resultado, relatorio.porResultado().getOrDefault(resultado, 0)))
                        .toList(),
                relatorio.porOperador(),
                relatorio.caixas().stream().map(CaixaResponse::de).toList());
    }
}

package com.empresax.sistema.relatorios.web;

import com.empresax.sistema.relatorios.caixa.CaixaDoPeriodo;
import com.empresax.sistema.relatorios.caixa.DinheiroDoDia;
import com.empresax.sistema.relatorios.caixa.ResultadoFechamento;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** Conferência do dinheiro de um dia: total do dia, cada caixa e os produtos vendidos em dinheiro. */
public record DinheiroDoDiaResponse(
        LocalDate dia,
        BigDecimal vendidoEmDinheiro,
        BigDecimal entrouNasGavetas,
        BigDecimal diferenca,
        /** Vazio = nenhum caixa fechado no dia ainda. */
        ResultadoFechamento resultado,
        int caixasAbertos,
        List<CaixaDoDiaResponse> caixas,
        List<DinheiroDoDia.ProdutoEmDinheiro> produtosEmDinheiro
) {

    public record CaixaDoDiaResponse(
            String pontoNome,
            String operadorNome,
            Instant abertaEm,
            Instant fechadaEm,
            BigDecimal valorInicial,
            BigDecimal reposicoes,
            BigDecimal sangrias,
            BigDecimal contado,
            BigDecimal entrouNaGaveta,
            BigDecimal vendidoEmDinheiro,
            BigDecimal diferenca,
            ResultadoFechamento resultado
    ) {

        static CaixaDoDiaResponse de(CaixaDoPeriodo caixa) {
            return new CaixaDoDiaResponse(
                    caixa.pontoNome(), caixa.operadorNome(), caixa.abertaEm(), caixa.fechadaEm(),
                    caixa.fundoInicial(), caixa.reposicoes(), caixa.sangrias(), caixa.valorContado(),
                    caixa.dinheiroQueEntrou().orElse(null), caixa.vendasEmDinheiro(),
                    caixa.diferenca().orElse(null), caixa.resultado());
        }
    }

    static DinheiroDoDiaResponse de(DinheiroDoDia dia) {
        return new DinheiroDoDiaResponse(
                dia.dia(),
                dia.vendidoEmDinheiro(),
                dia.entrouNasGavetas(),
                dia.diferenca(),
                dia.resultado().orElse(null),
                dia.caixasAbertos(),
                dia.caixas().stream().map(CaixaDoDiaResponse::de).toList(),
                dia.produtosEmDinheiro());
    }
}

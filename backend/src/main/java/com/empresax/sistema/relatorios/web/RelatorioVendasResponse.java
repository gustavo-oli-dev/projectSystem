package com.empresax.sistema.relatorios.web;

import com.empresax.sistema.relatorios.vendas.RelatorioVendas;
import com.empresax.sistema.relatorios.vendas.TotaisVendas;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record RelatorioVendasResponse(
        LocalDate inicio,
        LocalDate fim,
        String granularidade,
        Resumo resumo,
        Resumo periodoAnterior,
        Recorte desfeitas,
        BigDecimal recebido,
        BigDecimal aReceber,
        List<Ponto> serie,
        List<Recorte> porCanal,
        List<Recorte> porFormaPagamento,
        List<Recorte> porHora,
        List<Recorte> porDiaDaSemana,
        List<Produto> maisVendidos
) {

    /** margem nula = nenhum item vendido com custo informado. coberturaCusto: 0 a 1. */
    public record Resumo(
            BigDecimal faturamento, long vendas, BigDecimal ticketMedio, long unidades,
            BigDecimal lucroBruto, BigDecimal margem, BigDecimal coberturaCusto
    ) {
        static Resumo de(TotaisVendas totais) {
            return new Resumo(totais.faturamento(), totais.vendas(), totais.ticketMedio(), totais.unidades(),
                    totais.lucroBruto(), totais.margem().orElse(null), totais.coberturaDoCusto());
        }
    }

    public record Ponto(LocalDate inicio, BigDecimal faturamento, long vendas, BigDecimal lucroBruto) {
    }

    public record Recorte(String chave, long vendas, BigDecimal valor) {
        static Recorte de(RelatorioVendas.Recorte recorte) {
            return new Recorte(recorte.chave(), recorte.vendas(), recorte.valor());
        }
    }

    public record Produto(
            UUID id, String descricao, String tipo, long unidades, BigDecimal faturamento, BigDecimal lucroBruto,
            boolean custoCompleto
    ) {
    }

    public static RelatorioVendasResponse de(RelatorioVendas relatorio) {
        BigDecimal faturamento = relatorio.totais().faturamento();
        return new RelatorioVendasResponse(
                relatorio.periodo().inicio(),
                relatorio.periodo().fim(),
                relatorio.granularidade().name(),
                Resumo.de(relatorio.totais()),
                Resumo.de(relatorio.totaisPeriodoAnterior()),
                Recorte.de(relatorio.desfeitas()),
                relatorio.recebido(),
                faturamento.subtract(relatorio.recebido()).max(BigDecimal.ZERO),
                relatorio.serie().stream()
                        .map(ponto -> new Ponto(ponto.inicio(), ponto.totais().faturamento(), ponto.totais().vendas(),
                                ponto.totais().lucroBruto()))
                        .toList(),
                relatorio.porCanal().stream().map(Recorte::de).toList(),
                relatorio.porFormaPagamento().stream().map(Recorte::de).toList(),
                relatorio.porHora().stream().map(Recorte::de).toList(),
                relatorio.porDiaDaSemana().stream().map(Recorte::de).toList(),
                relatorio.maisVendidos().stream()
                        .map(item -> new Produto(item.id(), item.descricao(), item.tipo(), item.unidades(),
                                item.faturamento(), item.lucroBruto(), item.custoCompleto()))
                        .toList());
    }
}

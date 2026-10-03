package com.empresax.sistema.relatorios.vendas;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Tudo que o painel de vendas mostra para um período, calculado de uma vez (os números batem entre si). */
public record RelatorioVendas(
        PeriodoRelatorio periodo,
        Granularidade granularidade,
        TotaisVendas totais,
        TotaisVendas totaisPeriodoAnterior,
        Recorte desfeitas,
        BigDecimal recebido,
        List<PontoSerie> serie,
        List<Recorte> porCanal,
        List<Recorte> porFormaPagamento,
        List<Recorte> porHora,
        List<Recorte> porDiaDaSemana,
        List<ProdutoVendido> maisVendidos
) {

    /** Uma barra do gráfico no tempo: o dia (ou semana/mês) em que começa e o que vendeu. */
    public record PontoSerie(LocalDate inicio, TotaisVendas totais) {
    }

    /** Uma fatia de um agrupamento (canal, forma de pagamento, hora, dia da semana). */
    public record Recorte(String chave, long vendas, BigDecimal valor) {
    }

    /**
     * lucroBruto: só das vendas com custo informado (nulo se nenhuma tinha). custoCompleto = false
     * quando parte das vendas não tinha custo — o lucro mostrado é parcial.
     */
    public record ProdutoVendido(
            UUID id, String descricao, String tipo, long unidades, BigDecimal faturamento, BigDecimal lucroBruto,
            boolean custoCompleto
    ) {
    }
}

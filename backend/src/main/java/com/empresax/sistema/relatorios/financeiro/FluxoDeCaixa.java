package com.empresax.sistema.relatorios.financeiro;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Fluxo de caixa do período (D40): o que entrou (vendas recebidas, por forma) e o que saiu (contas
 * pagas), dia a dia. "A pagar" são as contas abertas que vencem no período — ainda não saíram.
 */
public record FluxoDeCaixa(List<Entrada> entradas, List<Saida> saidas, BigDecimal aPagarNoPeriodo) {

    public FluxoDeCaixa {
        entradas = List.copyOf(entradas);
        saidas = List.copyOf(saidas);
    }

    /** Recebido num dia numa forma (DINHEIRO, CARTAO_CREDITO, ..., PIX_ONLINE, BOLETO_ONLINE). */
    public record Entrada(LocalDate dia, String forma, BigDecimal valor) {
    }

    /** Uma conta paga: quando, o quê, para quem (pode não ter contato). */
    public record Saida(LocalDate dia, String descricao, String contato, BigDecimal valor) {
    }

    public record Dia(LocalDate dia, BigDecimal entradas, BigDecimal saidas) {

        public BigDecimal saldo() {
            return entradas.subtract(saidas);
        }
    }

    public BigDecimal totalEntradas() {
        return entradas.stream().map(Entrada::valor).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal totalSaidas() {
        return saidas.stream().map(Saida::valor).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal saldo() {
        return totalEntradas().subtract(totalSaidas());
    }

    /** Total recebido em cada forma no período. */
    public Map<String, BigDecimal> entradasPorForma() {
        Map<String, BigDecimal> porForma = new TreeMap<>();
        entradas.forEach(entrada -> porForma.merge(entrada.forma(), entrada.valor(), BigDecimal::add));
        return porForma;
    }

    /** Só os dias com movimento, em ordem. */
    public List<Dia> porDia() {
        Map<LocalDate, BigDecimal[]> dias = new TreeMap<>();
        entradas.forEach(entrada -> somar(dias, entrada.dia(), 0, entrada.valor()));
        saidas.forEach(saida -> somar(dias, saida.dia(), 1, saida.valor()));
        return dias.entrySet().stream()
                .map(dia -> new Dia(dia.getKey(), dia.getValue()[0], dia.getValue()[1]))
                .toList();
    }

    private static void somar(Map<LocalDate, BigDecimal[]> dias, LocalDate dia, int posicao, BigDecimal valor) {
        BigDecimal[] totais = dias.computeIfAbsent(dia, chave -> new BigDecimal[] {BigDecimal.ZERO, BigDecimal.ZERO});
        totais[posicao] = totais[posicao].add(valor);
    }
}

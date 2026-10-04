package com.empresax.sistema.relatorios.caixa;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Estatísticas da gestão de caixa num período (D28): quantos caixas, quantos bateram, sobras,
 * faltas, sangrias e reposições — no total e por operador. Diferença só conta caixa fechado.
 */
public record RelatorioCaixa(
        Totais totais,
        Map<ResultadoFechamento, Integer> porResultado,
        List<PorOperador> porOperador,
        List<CaixaDoPeriodo> caixas
) {

    public RelatorioCaixa {
        porResultado = Map.copyOf(porResultado);
        porOperador = List.copyOf(porOperador);
        caixas = List.copyOf(caixas);
    }

    public record Totais(
            int caixas,
            int fechados,
            BigDecimal faltas,
            BigDecimal sobras,
            BigDecimal saldo,
            BigDecimal sangrias,
            BigDecimal reposicoes,
            BigDecimal vendasEmDinheiro
    ) {
    }

    public record PorOperador(
            String operador,
            String operadorNome,
            int caixas,
            int fechados,
            int comFalta,
            BigDecimal faltas,
            BigDecimal sobras,
            BigDecimal saldo,
            BigDecimal sangrias,
            BigDecimal reposicoes
    ) {
    }

    public static RelatorioCaixa de(List<CaixaDoPeriodo> caixas) {
        Map<ResultadoFechamento, Integer> porResultado = new EnumMap<>(ResultadoFechamento.class);
        for (ResultadoFechamento resultado : ResultadoFechamento.values()) {
            porResultado.put(resultado, 0);
        }
        caixas.forEach(caixa -> porResultado.merge(caixa.resultado(), 1, Integer::sum));

        Map<String, List<CaixaDoPeriodo>> agrupados = caixas.stream()
                .collect(Collectors.groupingBy(CaixaDoPeriodo::operador, LinkedHashMap::new, Collectors.toList()));
        List<PorOperador> porOperador = agrupados.values().stream()
                .map(RelatorioCaixa::resumirOperador)
                // Quem mais faltou primeiro: é o que o gerente procura.
                .sorted(Comparator.comparing(PorOperador::faltas).reversed().thenComparing(PorOperador::operadorNome))
                .toList();

        return new RelatorioCaixa(totalizar(caixas), porResultado, porOperador, caixas);
    }

    private static Totais totalizar(List<CaixaDoPeriodo> caixas) {
        return new Totais(
                caixas.size(),
                (int) caixas.stream().filter(CaixaDoPeriodo::fechado).count(),
                faltas(caixas),
                sobras(caixas),
                saldo(caixas),
                somar(caixas, CaixaDoPeriodo::sangrias),
                somar(caixas, CaixaDoPeriodo::reposicoes),
                somar(caixas.stream().filter(CaixaDoPeriodo::fechado).toList(), CaixaDoPeriodo::vendasEmDinheiro));
    }

    private static PorOperador resumirOperador(List<CaixaDoPeriodo> doOperador) {
        CaixaDoPeriodo primeiro = doOperador.get(0);
        return new PorOperador(
                primeiro.operador(),
                primeiro.operadorNome(),
                doOperador.size(),
                (int) doOperador.stream().filter(CaixaDoPeriodo::fechado).count(),
                (int) doOperador.stream().filter(caixa -> caixa.resultado() == ResultadoFechamento.FALTOU).count(),
                faltas(doOperador),
                sobras(doOperador),
                saldo(doOperador),
                somar(doOperador, CaixaDoPeriodo::sangrias),
                somar(doOperador, CaixaDoPeriodo::reposicoes));
    }

    /** Total que faltou, como número positivo. */
    private static BigDecimal faltas(List<CaixaDoPeriodo> caixas) {
        return diferencas(caixas).filter(diferenca -> diferenca.signum() < 0)
                .map(BigDecimal::negate).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static BigDecimal sobras(List<CaixaDoPeriodo> caixas) {
        return diferencas(caixas).filter(diferenca -> diferenca.signum() > 0).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Sobras − faltas: negativo = no fim das contas, faltou dinheiro. */
    private static BigDecimal saldo(List<CaixaDoPeriodo> caixas) {
        return diferencas(caixas).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static Stream<BigDecimal> diferencas(List<CaixaDoPeriodo> caixas) {
        return caixas.stream().flatMap(caixa -> caixa.diferenca().stream());
    }

    private static BigDecimal somar(List<CaixaDoPeriodo> caixas, Function<CaixaDoPeriodo, BigDecimal> valor) {
        return caixas.stream().map(valor).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}

package com.empresax.sistema.relatorios.estoque;

import java.math.BigDecimal;
import java.util.List;

/**
 * Perdas e quebras no período (D32): o que saiu do estoque sem ser vendido — por motivo e por
 * produto. Falta achada no inventário entra como motivo "INVENTARIO" (perda sem explicação).
 * Valor = quantidade × custo do produto na hora; sem custo cadastrado, a unidade conta mas o valor não.
 */
public record RelatorioPerdas(
        int unidades,
        BigDecimal valor,
        int unidadesSemCusto,
        List<PorMotivo> porMotivo,
        List<PorProduto> porProduto
) {

    public static final String MOTIVO_INVENTARIO = "INVENTARIO";

    public RelatorioPerdas {
        porMotivo = List.copyOf(porMotivo);
        porProduto = List.copyOf(porProduto);
    }

    public record PorMotivo(String motivo, int unidades, BigDecimal valor) {
    }

    public record PorProduto(String produto, int unidades, BigDecimal valor) {
    }

    public static RelatorioPerdas de(List<PorMotivo> porMotivo, List<PorProduto> porProduto, int unidadesSemCusto) {
        int unidades = porMotivo.stream().mapToInt(PorMotivo::unidades).sum();
        BigDecimal valor = porMotivo.stream().map(PorMotivo::valor).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new RelatorioPerdas(unidades, valor, unidadesSemCusto, porMotivo, porProduto);
    }
}

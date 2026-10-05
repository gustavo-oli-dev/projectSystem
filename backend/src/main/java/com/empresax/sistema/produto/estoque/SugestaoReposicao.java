package com.empresax.sistema.produto.estoque;

import com.empresax.sistema.produto.Produto;

import java.util.UUID;

/**
 * Quanto comprar de um produto que chegou no estoque mínimo (D33): o suficiente para cobrir os
 * próximos 30 dias no ritmo dos últimos 30 e ainda sobrar o mínimo. Nunca sugere menos de 1.
 */
public record SugestaoReposicao(
        UUID produtoId,
        String nome,
        String codigoBarras,
        String unidadeMedida,
        int estoque,
        int estoqueMinimo,
        boolean minimoDefinido,
        int vendidosEm30Dias,
        int quantidadeSugerida
) {

    public static final int DIAS_DE_COBERTURA = 30;
    private static final int SUGESTAO_MINIMA = 1;

    public static SugestaoReposicao para(Produto produto, int vendidosEm30Dias) {
        int minimo = produto.estoqueMinimoEfetivo();
        int sugerida = Math.max(SUGESTAO_MINIMA, vendidosEm30Dias + minimo - produto.quantidadeEmEstoque());
        return new SugestaoReposicao(
                produto.id(), produto.nome(), produto.codigoBarras().orElse(null), produto.unidadeMedida(),
                produto.quantidadeEmEstoque(), minimo, produto.estoqueMinimo().isPresent(), vendidosEm30Dias, sugerida);
    }
}

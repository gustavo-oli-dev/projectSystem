package com.empresax.sistema.produto.web;

import com.empresax.sistema.produto.estoque.MovimentacaoEstoque;

import java.time.Instant;
import java.util.UUID;

public record MovimentacaoEstoqueResponse(
        UUID id,
        String tipo,
        int quantidade,
        int saldoApos,
        UUID pedidoId,
        String responsavel,
        Instant criadaEm
) {

    public static MovimentacaoEstoqueResponse de(MovimentacaoEstoque movimentacao) {
        return new MovimentacaoEstoqueResponse(
                movimentacao.id(),
                movimentacao.tipo().name(),
                movimentacao.quantidade(),
                movimentacao.saldoApos(),
                movimentacao.pedidoId().orElse(null),
                movimentacao.responsavel(),
                movimentacao.criadaEm());
    }
}

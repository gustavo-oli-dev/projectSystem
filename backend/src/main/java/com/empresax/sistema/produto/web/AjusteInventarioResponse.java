package com.empresax.sistema.produto.web;

import com.empresax.sistema.produto.estoque.EstoqueService.AjusteInventario;

import java.util.UUID;

public record AjusteInventarioResponse(UUID produtoId, String nome, int noSistema, int contado, int diferenca) {

    static AjusteInventarioResponse de(AjusteInventario ajuste) {
        return new AjusteInventarioResponse(ajuste.produtoId(), ajuste.nome(), ajuste.noSistema(), ajuste.contado(), ajuste.diferenca());
    }
}

package com.empresax.sistema.loja.web;

import com.empresax.sistema.produto.Produto;
import com.empresax.sistema.produto.web.ProdutoResponse;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record ProdutoVitrineResponse(
        UUID id,
        String nome,
        String descricao,
        BigDecimal preco,
        boolean disponivel,
        int quantidadeDisponivel,
        List<String> fotos
) {

    public static ProdutoVitrineResponse de(Produto produto, List<UUID> idsDasFotos) {
        List<String> urls = idsDasFotos.stream()
                .map(fotoId -> ProdutoResponse.urlDaFoto(produto.id(), fotoId))
                .toList();
        return new ProdutoVitrineResponse(
                produto.id(),
                produto.nome(),
                produto.descricao(),
                produto.precoUnitario().valor(),
                produto.quantidadeEmEstoque() > 0,
                produto.quantidadeEmEstoque(),
                urls);
    }
}

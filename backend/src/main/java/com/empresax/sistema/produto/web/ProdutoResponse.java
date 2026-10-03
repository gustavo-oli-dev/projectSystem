package com.empresax.sistema.produto.web;

import com.empresax.sistema.produto.Produto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record ProdutoResponse(
        UUID id,
        String nome,
        String descricao,
        String ncm,
        String unidadeMedida,
        BigDecimal precoUnitario,
        boolean ativo,
        String codigoBarras,
        int quantidadeEmEstoque,
        List<FotoResponse> fotos
) {

    public record FotoResponse(UUID id, String url) {
    }

    public static ProdutoResponse de(Produto produto, List<UUID> idsDasFotos) {
        List<FotoResponse> fotos = idsDasFotos.stream()
                .map(fotoId -> new FotoResponse(fotoId, urlDaFoto(produto.id(), fotoId)))
                .toList();
        return new ProdutoResponse(
                produto.id(),
                produto.nome(),
                produto.descricao(),
                produto.ncm(),
                produto.unidadeMedida(),
                produto.precoUnitario().valor(),
                produto.ativo(),
                produto.codigoBarras().orElse(null),
                produto.quantidadeEmEstoque(),
                fotos
        );
    }

    public static String urlDaFoto(UUID produtoId, UUID fotoId) {
        return "/api/produtos/" + produtoId + "/fotos/" + fotoId;
    }
}

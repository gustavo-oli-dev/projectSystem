package com.empresax.sistema.produto.web;

import com.empresax.sistema.produto.Produto;
import com.empresax.sistema.produto.embalagem.Embalagem;
import com.empresax.sistema.shared.dinheiro.Dinheiro;

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
        /** Vazio = não definido (vale o aviso padrão de 5). */
        Integer estoqueMinimo,
        List<FotoResponse> fotos,
        /** Só preenchido para quem gerencia o catálogo ou vê o faturamento (custo é dado sensível). */
        BigDecimal custoUnitario,
        /** Formas de vender em quantidade (ex.: fardo com 12) — D41. */
        List<EmbalagemResponse> embalagens
) {

    public record EmbalagemResponse(UUID id, String nome, String codigoBarras, int unidades, BigDecimal preco) {

        static EmbalagemResponse de(Embalagem embalagem) {
            return new EmbalagemResponse(embalagem.id(), embalagem.nome(), embalagem.codigoBarras().orElse(null),
                    embalagem.unidades(), embalagem.preco().valor());
        }
    }

    public record FotoResponse(UUID id, String url) {
    }

    public static ProdutoResponse de(Produto produto, List<UUID> idsDasFotos) {
        return de(produto, idsDasFotos, false);
    }

    public static ProdutoResponse de(Produto produto, List<UUID> idsDasFotos, boolean mostrarCusto) {
        return de(produto, idsDasFotos, mostrarCusto, List.of());
    }

    public static ProdutoResponse de(Produto produto, List<UUID> idsDasFotos, boolean mostrarCusto, List<Embalagem> embalagens) {
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
                produto.estoqueMinimo().orElse(null),
                fotos,
                mostrarCusto ? produto.custoUnitario().map(Dinheiro::valor).orElse(null) : null,
                embalagens.stream().map(EmbalagemResponse::de).toList()
        );
    }

    public static String urlDaFoto(UUID produtoId, UUID fotoId) {
        return "/api/produtos/" + produtoId + "/fotos/" + fotoId;
    }
}

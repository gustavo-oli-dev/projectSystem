package com.empresax.sistema.loja.web;

import com.empresax.sistema.produto.ProdutoService;
import com.empresax.sistema.produto.foto.FotoProdutoService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Vitrine para o site de vendas (D18). Público e só leitura (justificado em SecurityConfig): mostra
 * apenas produtos ativos e só o que um cliente precisa ver — sem NCM, custo ou dados internos.
 */
@RestController
@RequestMapping("/api/loja")
public class LojaController {

    private final ProdutoService produtoService;
    private final FotoProdutoService fotoProdutoService;

    public LojaController(ProdutoService produtoService, FotoProdutoService fotoProdutoService) {
        this.produtoService = produtoService;
        this.fotoProdutoService = fotoProdutoService;
    }

    @GetMapping("/produtos")
    public List<ProdutoVitrineResponse> produtos() {
        Map<UUID, List<UUID>> fotos = fotoProdutoService.idsDasFotosPorProduto();
        return produtoService.listarAtivos().stream()
                .map(produto -> ProdutoVitrineResponse.de(produto, fotos.getOrDefault(produto.id(), List.of())))
                .toList();
    }
}

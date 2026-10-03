package com.empresax.sistema.produto.web;

import com.empresax.sistema.acesso.RegraAcesso;
import com.empresax.sistema.produto.Produto;
import com.empresax.sistema.produto.estoque.EstoqueService;
import com.empresax.sistema.produto.foto.FotoProdutoService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/produtos/{produtoId}/estoque")
public class EstoqueController {

    private final EstoqueService estoqueService;
    private final FotoProdutoService fotoProdutoService;

    public EstoqueController(EstoqueService estoqueService, FotoProdutoService fotoProdutoService) {
        this.estoqueService = estoqueService;
        this.fotoProdutoService = fotoProdutoService;
    }

    @PreAuthorize(RegraAcesso.ESTOQUE_GERENCIAR)
    @PostMapping("/entradas")
    public ProdutoResponse darEntrada(
            @PathVariable UUID produtoId,
            @Valid @RequestBody EntradaEstoqueRequest requisicao,
            @AuthenticationPrincipal UserDetails usuario
    ) {
        Produto produto = estoqueService.darEntrada(produtoId, requisicao.quantidade(), usuario.getUsername());
        return ProdutoResponse.de(produto, fotoProdutoService.idsDasFotosPorProduto().getOrDefault(produtoId, List.of()));
    }

    @PreAuthorize(RegraAcesso.CATALOGO_VER)
    @GetMapping("/movimentacoes")
    public List<MovimentacaoEstoqueResponse> movimentacoes(@PathVariable UUID produtoId) {
        return estoqueService.ultimasMovimentacoes(produtoId).stream()
                .map(MovimentacaoEstoqueResponse::de)
                .toList();
    }
}

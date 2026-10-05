package com.empresax.sistema.produto.web;

import com.empresax.sistema.acesso.RegraAcesso;
import com.empresax.sistema.produto.estoque.EstoqueService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/** Inventário: acerta o estoque de vários produtos à contagem da prateleira, numa operação só. */
@RestController
@RequestMapping("/api/estoque/inventario")
public class InventarioController {

    private final EstoqueService estoqueService;

    public InventarioController(EstoqueService estoqueService) {
        this.estoqueService = estoqueService;
    }

    @PreAuthorize(RegraAcesso.ESTOQUE_GERENCIAR)
    @PostMapping
    public List<AjusteInventarioResponse> aplicar(@Valid @RequestBody InventarioRequest requisicao, @AuthenticationPrincipal UserDetails usuario) {
        Map<UUID, Integer> contagens = requisicao.contagens().stream().collect(Collectors.toMap(
                InventarioRequest.ContagemProduto::produtoId, InventarioRequest.ContagemProduto::quantidadeContada,
                (primeira, repetida) -> repetida));
        return estoqueService.aplicarInventario(contagens, usuario.getUsername()).stream()
                .map(AjusteInventarioResponse::de)
                .toList();
    }
}

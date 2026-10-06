package com.empresax.sistema.produto.web;

import com.empresax.sistema.acesso.RegraAcesso;
import com.empresax.sistema.produto.estoque.EstoqueService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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

/** Entrada pelo leitor (D46): o que chegou, lido produto a produto, entra no estoque de uma vez. */
@RestController
@RequestMapping("/api/estoque/entradas")
public class EntradaEmLoteController {

    private final EstoqueService estoqueService;

    public EntradaEmLoteController(EstoqueService estoqueService) {
        this.estoqueService = estoqueService;
    }

    public record EntradaEmLoteRequest(
            @NotEmpty(message = "Leia ao menos um produto") @Size(max = 2000, message = "Produtos demais de uma vez")
            List<@Valid @NotNull Item> itens
    ) {

        public record Item(
                @NotNull(message = "Produto não informado") UUID produtoId,
                @Min(value = 1, message = "A quantidade precisa ser ao menos 1")
                @Max(value = 100_000, message = "Quantidade fora do limite") int quantidade
        ) {
        }
    }

    public record EstoqueAtualizadoResponse(UUID produtoId, String produto, int quantidadeEmEstoque) {
    }

    @PreAuthorize(RegraAcesso.ESTOQUE_GERENCIAR)
    @PostMapping
    public List<EstoqueAtualizadoResponse> darEntrada(
            @Valid @RequestBody EntradaEmLoteRequest requisicao, @AuthenticationPrincipal UserDetails usuario
    ) {
        // O mesmo produto repetido na lista soma (não sobrescreve).
        Map<UUID, Integer> quantidades = requisicao.itens().stream()
                .collect(Collectors.toMap(EntradaEmLoteRequest.Item::produtoId, EntradaEmLoteRequest.Item::quantidade, Integer::sum));
        return estoqueService.darEntradaEmLote(quantidades, usuario.getUsername()).stream()
                .map(produto -> new EstoqueAtualizadoResponse(produto.id(), produto.nome(), produto.quantidadeEmEstoque()))
                .toList();
    }
}

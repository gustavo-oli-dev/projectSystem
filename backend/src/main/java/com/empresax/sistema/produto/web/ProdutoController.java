package com.empresax.sistema.produto.web;

import com.empresax.sistema.acesso.RegraAcesso;
import com.empresax.sistema.produto.Produto;
import com.empresax.sistema.produto.ProdutoService;
import com.empresax.sistema.produto.foto.FotoProdutoService;
import com.empresax.sistema.shared.dinheiro.Dinheiro;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/produtos")
public class ProdutoController {

    private final ProdutoService produtoService;
    private final FotoProdutoService fotoProdutoService;

    public ProdutoController(ProdutoService produtoService, FotoProdutoService fotoProdutoService) {
        this.produtoService = produtoService;
        this.fotoProdutoService = fotoProdutoService;
    }

    @PreAuthorize(RegraAcesso.CATALOGO_GERENCIAR)
    @PostMapping
    public ResponseEntity<ProdutoResponse> cadastrar(@Valid @RequestBody CriarProdutoRequest requisicao) {
        Produto produto = produtoService.cadastrar(
                requisicao.nome(),
                requisicao.descricao(),
                requisicao.ncm(),
                requisicao.unidadeMedida(),
                new Dinheiro(requisicao.precoUnitario()),
                requisicao.codigoBarras());
        ProdutoResponse resposta = ProdutoResponse.de(produto, List.of());
        return ResponseEntity.created(URI.create("/api/produtos/" + produto.id())).body(resposta);
    }

    @PreAuthorize(RegraAcesso.CATALOGO_VER)
    @GetMapping
    public List<ProdutoResponse> listar() {
        Map<UUID, List<UUID>> fotos = fotoProdutoService.idsDasFotosPorProduto();
        return produtoService.listarTodos().stream()
                .map(produto -> ProdutoResponse.de(produto, fotos.getOrDefault(produto.id(), List.of())))
                .toList();
    }

    @PreAuthorize(RegraAcesso.CATALOGO_VER)
    @GetMapping("/{id}")
    public ProdutoResponse buscarPorId(@PathVariable UUID id) {
        return comFotos(produtoService.buscarPorId(id));
    }

    @PreAuthorize(RegraAcesso.CATALOGO_GERENCIAR)
    @PutMapping("/{id}")
    public ProdutoResponse atualizar(@PathVariable UUID id, @Valid @RequestBody AtualizarProdutoRequest requisicao) {
        Produto produto = produtoService.atualizar(
                id, requisicao.nome(), requisicao.descricao(), new Dinheiro(requisicao.precoUnitario()),
                requisicao.codigoBarras());
        return comFotos(produto);
    }

    @PreAuthorize(RegraAcesso.CATALOGO_GERENCIAR)
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void desativar(@PathVariable UUID id) {
        produtoService.desativar(id);
    }

    @PreAuthorize(RegraAcesso.CATALOGO_GERENCIAR)
    @PostMapping("/{id}/ativar")
    public ProdutoResponse ativar(@PathVariable UUID id) {
        return comFotos(produtoService.ativar(id));
    }

    private ProdutoResponse comFotos(Produto produto) {
        return ProdutoResponse.de(produto, fotoProdutoService.idsDasFotosPorProduto().getOrDefault(produto.id(), List.of()));
    }
}

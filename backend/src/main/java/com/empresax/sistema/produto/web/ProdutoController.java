package com.empresax.sistema.produto.web;

import com.empresax.sistema.acesso.Permissao;
import com.empresax.sistema.acesso.RegraAcesso;
import com.empresax.sistema.produto.Produto;
import com.empresax.sistema.produto.ProdutoService;
import com.empresax.sistema.produto.foto.FotoProdutoService;
import com.empresax.sistema.shared.dinheiro.Dinheiro;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/produtos")
public class ProdutoController {

    private static final Set<String> PERMISSOES_QUE_VEEM_CUSTO =
            Set.of(Permissao.CATALOGO_GERENCIAR.name(), Permissao.FATURAMENTO_VER.name());

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
                requisicao.codigoBarras(),
                dinheiroOuNulo(requisicao.custoUnitario()));
        ProdutoResponse resposta = ProdutoResponse.de(produto, List.of(), podeVerCusto());
        return ResponseEntity.created(URI.create("/api/produtos/" + produto.id())).body(resposta);
    }

    /** O caixa também lista (busca pelo nome quando o produto não tem código de barras). */
    @PreAuthorize(RegraAcesso.CATALOGO_VER + " or " + RegraAcesso.PDV_VENDER)
    @GetMapping
    public List<ProdutoResponse> listar() {
        Map<UUID, List<UUID>> fotos = fotoProdutoService.idsDasFotosPorProduto();
        return produtoService.listarTodos().stream()
                .map(produto -> ProdutoResponse.de(produto, fotos.getOrDefault(produto.id(), List.of()), podeVerCusto()))
                .toList();
    }

    @PreAuthorize(RegraAcesso.CATALOGO_VER + " or " + RegraAcesso.PDV_VENDER + " or " + RegraAcesso.ESTOQUE_GERENCIAR)
    @GetMapping("/codigo-barras/{codigo}")
    public ProdutoResponse buscarPorCodigoBarras(@PathVariable String codigo) {
        return comFotos(produtoService.buscarPorCodigoBarras(codigo));
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
                requisicao.codigoBarras(), dinheiroOuNulo(requisicao.custoUnitario()));
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
        List<UUID> fotos = fotoProdutoService.idsDasFotosPorProduto().getOrDefault(produto.id(), List.of());
        return ProdutoResponse.de(produto, fotos, podeVerCusto());
    }

    /** Custo é dado sensível: só quem gerencia o catálogo ou vê o faturamento (o caixa, não). */
    private static boolean podeVerCusto() {
        Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
        return autenticacao != null && autenticacao.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(PERMISSOES_QUE_VEEM_CUSTO::contains);
    }

    private static Dinheiro dinheiroOuNulo(BigDecimal valor) {
        return valor == null ? null : new Dinheiro(valor);
    }
}

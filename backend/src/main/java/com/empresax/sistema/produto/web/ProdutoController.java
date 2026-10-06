package com.empresax.sistema.produto.web;

import com.empresax.sistema.acesso.Permissao;
import com.empresax.sistema.acesso.RegraAcesso;
import com.empresax.sistema.produto.Produto;
import com.empresax.sistema.produto.ProdutoService;
import com.empresax.sistema.produto.embalagem.Embalagem;
import com.empresax.sistema.produto.embalagem.EmbalagemService;
import com.empresax.sistema.produto.foto.FotoProdutoService;
import com.empresax.sistema.shared.dinheiro.Dinheiro;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
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
    private final EmbalagemService embalagemService;

    public ProdutoController(ProdutoService produtoService, FotoProdutoService fotoProdutoService, EmbalagemService embalagemService) {
        this.produtoService = produtoService;
        this.fotoProdutoService = fotoProdutoService;
        this.embalagemService = embalagemService;
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
        Map<UUID, List<Embalagem>> embalagens = embalagemService.ativasPorProduto();
        return produtoService.listarTodos().stream()
                .map(produto -> ProdutoResponse.de(produto, fotos.getOrDefault(produto.id(), List.of()), podeVerCusto(),
                        embalagens.getOrDefault(produto.id(), List.of())))
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
    public ProdutoResponse atualizar(
            @PathVariable UUID id, @Valid @RequestBody AtualizarProdutoRequest requisicao, @AuthenticationPrincipal UserDetails usuario
    ) {
        Produto produto = produtoService.atualizar(
                id, requisicao.nome(), requisicao.descricao(), new Dinheiro(requisicao.precoUnitario()),
                requisicao.codigoBarras(), dinheiroOuNulo(requisicao.custoUnitario()), requisicao.estoqueMinimo(),
                usuario.getUsername());
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

    public record EmbalagemRequest(
            @NotBlank(message = "Dê um nome à embalagem") @Size(max = 60, message = "Nome com no máximo 60 caracteres") String nome,
            @Size(max = 14, message = "Código de barras com no máximo 14 dígitos") String codigoBarras,
            @Min(value = 2, message = "A embalagem precisa ter ao menos 2 unidades")
            @Max(value = 1000, message = "No máximo 1000 unidades por embalagem") int unidades,
            @NotNull(message = "Informe o preço da embalagem")
            @DecimalMin(value = "0.01", message = "O preço precisa ser maior que zero") BigDecimal preco
    ) {
    }

    /** Embalagem (D41): ex.: "Fardo com 12", com preço e, se tiver, código de barras próprio. */
    @PreAuthorize(RegraAcesso.CATALOGO_GERENCIAR)
    @PostMapping("/{id}/embalagens")
    public ProdutoResponse adicionarEmbalagem(@PathVariable UUID id, @Valid @RequestBody EmbalagemRequest requisicao) {
        embalagemService.adicionar(id, requisicao.nome(), requisicao.codigoBarras(), requisicao.unidades(),
                new Dinheiro(requisicao.preco()));
        return comFotos(produtoService.buscarPorId(id));
    }

    @PreAuthorize(RegraAcesso.CATALOGO_GERENCIAR)
    @DeleteMapping("/{id}/embalagens/{embalagemId}")
    public ProdutoResponse removerEmbalagem(@PathVariable UUID id, @PathVariable UUID embalagemId) {
        embalagemService.remover(id, embalagemId);
        return comFotos(produtoService.buscarPorId(id));
    }

    private ProdutoResponse comFotos(Produto produto) {
        List<UUID> fotos = fotoProdutoService.idsDasFotosPorProduto().getOrDefault(produto.id(), List.of());
        return ProdutoResponse.de(produto, fotos, podeVerCusto(), embalagemService.ativasDo(produto.id()));
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

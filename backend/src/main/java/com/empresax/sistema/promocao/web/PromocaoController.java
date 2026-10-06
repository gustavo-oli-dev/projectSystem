package com.empresax.sistema.promocao.web;

import com.empresax.sistema.acesso.RegraAcesso;
import com.empresax.sistema.common.domain.DomainException;
import com.empresax.sistema.produto.Produto;
import com.empresax.sistema.produto.ProdutoService;
import com.empresax.sistema.promocao.Promocao;
import com.empresax.sistema.promocao.PromocaoService;
import com.empresax.sistema.shared.dinheiro.Dinheiro;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Promoções (D38). Ver as que valem hoje é do caixa; criar e encerrar exige permissão própria. */
@RestController
@RequestMapping("/api/promocoes")
public class PromocaoController {

    private final PromocaoService promocaoService;
    private final ProdutoService produtoService;

    public PromocaoController(PromocaoService promocaoService, ProdutoService produtoService) {
        this.promocaoService = promocaoService;
        this.produtoService = produtoService;
    }

    @PreAuthorize(RegraAcesso.PROMOCOES_GERENCIAR + " or " + RegraAcesso.CATALOGO_VER)
    @GetMapping
    public List<PromocaoResponse> listar() {
        return responder(promocaoService.listar());
    }

    /** O caixa mostra o preço com a promoção antes de fechar a venda (o servidor recalcula ao vender). */
    @PreAuthorize(RegraAcesso.PDV_VENDER + " or " + RegraAcesso.CATALOGO_VER)
    @GetMapping("/valendo-hoje")
    public List<PromocaoResponse> valendoHoje() {
        return responder(promocaoService.valendoHoje());
    }

    @PreAuthorize(RegraAcesso.PROMOCOES_GERENCIAR)
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PromocaoResponse criar(@Valid @RequestBody PromocaoRequest requisicao, @AuthenticationPrincipal UserDetails usuario) {
        Promocao promocao = switch (requisicao.tipo()) {
            case PRECO_OFERTA -> promocaoService.criarPrecoDeOferta(requisicao.produtoId(),
                    new Dinheiro(exigir(requisicao.precoOferta(), "Informe o preço de oferta")),
                    requisicao.inicio(), requisicao.fim(), usuario.getUsername());
            case LEVE_PAGUE -> promocaoService.criarLevePague(requisicao.produtoId(),
                    exigir(requisicao.leve(), "Informe quantas unidades o cliente leva"),
                    exigir(requisicao.pague(), "Informe quantas unidades o cliente paga"),
                    requisicao.inicio(), requisicao.fim(), usuario.getUsername());
        };
        return responder(List.of(promocao)).getFirst();
    }

    @PreAuthorize(RegraAcesso.PROMOCOES_GERENCIAR)
    @PostMapping("/{promocaoId}/encerrar")
    public PromocaoResponse encerrar(@PathVariable UUID promocaoId, @AuthenticationPrincipal UserDetails usuario) {
        return responder(List.of(promocaoService.encerrar(promocaoId, usuario.getUsername()))).getFirst();
    }

    private static <T> T exigir(T valor, String mensagem) {
        if (valor == null) {
            throw new DomainException(mensagem);
        }
        return valor;
    }

    /** Nome e preço normal de cada produto, resolvidos de uma vez (sem N+1). */
    private List<PromocaoResponse> responder(List<Promocao> promocoes) {
        Map<UUID, Produto> produtos = produtoService.listarTodos().stream()
                .collect(Collectors.toMap(Produto::id, Function.identity()));
        LocalDate hoje = promocaoService.hoje();
        return promocoes.stream()
                .filter(promocao -> produtos.containsKey(promocao.produtoId()))
                .map(promocao -> {
                    Produto produto = produtos.get(promocao.produtoId());
                    return PromocaoResponse.de(promocao, produto.nome(), produto.precoUnitario(), hoje);
                })
                .toList();
    }
}

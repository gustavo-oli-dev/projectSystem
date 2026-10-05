package com.empresax.sistema.pdv.autorizacao.web;

import com.empresax.sistema.acesso.RegraAcesso;
import com.empresax.sistema.pdv.autorizacao.AcaoAutorizada;
import com.empresax.sistema.pdv.autorizacao.AutorizacaoCaixaService;
import com.empresax.sistema.pdv.autorizacao.AutorizacaoCaixaService.AutorizacaoEmitida;
import com.empresax.sistema.pdv.autorizacao.CancelamentoItemService;
import com.empresax.sistema.pdv.autorizacao.ItemCanceladoCaixa;
import com.empresax.sistema.relatorios.vendas.PeriodoRelatorio;
import com.empresax.sistema.usuario.UsuarioService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Autorização do gerente no caixa (D35): o operador pede (o gerente digita e-mail e senha), recebe
 * um token curto e usa no desconto ou no cancelamento de item. Os cancelamentos vão para o relatório.
 */
@RestController
public class AutorizacaoController {

    private final AutorizacaoCaixaService autorizacaoService;
    private final CancelamentoItemService cancelamentoService;
    private final UsuarioService usuarioService;

    public AutorizacaoController(
            AutorizacaoCaixaService autorizacaoService, CancelamentoItemService cancelamentoService, UsuarioService usuarioService
    ) {
        this.autorizacaoService = autorizacaoService;
        this.cancelamentoService = cancelamentoService;
        this.usuarioService = usuarioService;
    }

    public record AutorizacaoRequest(
            @NotBlank(message = "Informe o e-mail de quem autoriza") @Size(max = 255) String email,
            @NotBlank(message = "Informe a senha de quem autoriza") @Size(max = 200) String senha,
            @NotNull(message = "Informe o que está sendo autorizado") AcaoAutorizada acao
    ) {
    }

    public record AutorizacaoResponse(String token, String autorizadoPorNome, Instant expiraEm) {
    }

    public record CancelamentoItemRequest(
            @NotNull(message = "Informe o produto") UUID produtoId,
            @Min(value = 1, message = "Quantidade inválida") @Max(value = 100_000, message = "Quantidade inválida") int quantidade,
            @NotBlank(message = "Cancelar item precisa da autorização de um gerente") String tokenAutorizacao
    ) {
    }

    public record ItemCanceladoResponse(
            UUID id, String descricao, int quantidade, BigDecimal valor, String operadorNome, String autorizadoPorNome, Instant canceladoEm
    ) {
    }

    @PreAuthorize(RegraAcesso.PDV_VENDER)
    @PostMapping("/api/pdv/autorizacoes")
    public AutorizacaoResponse autorizar(@Valid @RequestBody AutorizacaoRequest requisicao, @AuthenticationPrincipal UserDetails operador) {
        AutorizacaoEmitida emitida = autorizacaoService.autorizar(
                requisicao.email(), requisicao.senha(), requisicao.acao(), operador.getUsername());
        return new AutorizacaoResponse(emitida.token(), emitida.autorizadoPorNome(), emitida.expiraEm());
    }

    @PreAuthorize(RegraAcesso.PDV_VENDER)
    @PostMapping("/api/pdv/itens-cancelados")
    @ResponseStatus(HttpStatus.CREATED)
    public ItemCanceladoResponse cancelarItem(
            @Valid @RequestBody CancelamentoItemRequest requisicao, @AuthenticationPrincipal UserDetails operador
    ) {
        ItemCanceladoCaixa cancelado = cancelamentoService.registrar(
                requisicao.produtoId(), requisicao.quantidade(), requisicao.tokenAutorizacao(), operador.getUsername());
        return responder(List.of(cancelado)).get(0);
    }

    @PreAuthorize(RegraAcesso.FATURAMENTO_VER + " or " + RegraAcesso.CAIXA_CONFERIR)
    @GetMapping("/api/relatorios/itens-cancelados")
    public List<ItemCanceladoResponse> doPeriodo(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim
    ) {
        return responder(cancelamentoService.doPeriodo(new PeriodoRelatorio(inicio, fim)));
    }

    private List<ItemCanceladoResponse> responder(List<ItemCanceladoCaixa> cancelados) {
        Map<String, String> nomes = usuarioService.nomesPorEmail(cancelados.stream()
                .flatMap(item -> Stream.of(item.operador(), item.autorizadoPor())).collect(Collectors.toSet()));
        return cancelados.stream().map(item -> new ItemCanceladoResponse(
                item.id(), item.descricao(), item.quantidade(), item.valor().valor(),
                nomes.getOrDefault(item.operador(), item.operador()), nomes.getOrDefault(item.autorizadoPor(), item.autorizadoPor()),
                item.canceladoEm())).toList();
    }
}

package com.empresax.sistema.produto.web;

import com.empresax.sistema.acesso.RegraAcesso;
import com.empresax.sistema.produto.preco.ModoReajuste;
import com.empresax.sistema.produto.preco.ReajustePrecoService;
import com.empresax.sistema.relatorios.vendas.PeriodoRelatorio;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Preço em lote e o que mudou de preço (para as etiquetas de gôndola) — D39. */
@RestController
@RequestMapping("/api/produtos")
@Validated
public class PrecoController {

    private static final int MAXIMO_DIAS = 90;

    private final ReajustePrecoService reajustePrecoService;
    private final Clock relogioDaLoja;

    public PrecoController(ReajustePrecoService reajustePrecoService, Clock relogioDaLoja) {
        this.reajustePrecoService = reajustePrecoService;
        this.relogioDaLoja = relogioDaLoja;
    }

    public record ReajusteRequest(
            @NotEmpty(message = "Escolha ao menos um produto")
            @Size(max = ReajustePrecoService.MAXIMO_DE_PRODUTOS, message = "Produtos demais de uma vez") List<@NotNull UUID> produtoIds,
            @NotNull(message = "Escolha como reajustar") ModoReajuste modo,
            @NotNull(message = "Informe o valor do reajuste") BigDecimal valor
    ) {
    }

    public record PrecoAlteradoResponse(UUID produtoId, String produto, BigDecimal precoAnterior, BigDecimal precoNovo) {
    }

    @PreAuthorize(RegraAcesso.CATALOGO_GERENCIAR)
    @PostMapping("/reajuste-precos")
    public List<PrecoAlteradoResponse> reajustar(@Valid @RequestBody ReajusteRequest requisicao, @AuthenticationPrincipal UserDetails usuario) {
        return reajustePrecoService.reajustar(requisicao.produtoIds(), requisicao.modo(), requisicao.valor(), usuario.getUsername())
                .stream()
                .map(alterado -> new PrecoAlteradoResponse(alterado.produtoId(), alterado.produto(),
                        alterado.precoAnterior().valor(), alterado.precoNovo().valor()))
                .toList();
    }

    /** Produtos que mudaram de preço nos últimos N dias (contando hoje), para reimprimir as etiquetas. */
    @PreAuthorize(RegraAcesso.CATALOGO_VER)
    @GetMapping("/precos-alterados")
    public List<UUID> alterados(
            @RequestParam(defaultValue = "7") @Min(value = 1, message = "Ao menos 1 dia") @Max(value = MAXIMO_DIAS, message = "No máximo 90 dias") int dias
    ) {
        LocalDate primeiroDia = LocalDate.now(relogioDaLoja).minusDays(dias - 1L);
        return List.copyOf(reajustePrecoService.alteradosDesde(
                primeiroDia.atStartOfDay(PeriodoRelatorio.FUSO_DA_LOJA).toInstant()));
    }
}

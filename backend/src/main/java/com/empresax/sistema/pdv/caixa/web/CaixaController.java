package com.empresax.sistema.pdv.caixa.web;

import com.empresax.sistema.acesso.RegraAcesso;
import com.empresax.sistema.pdv.caixa.CaixaService;
import com.empresax.sistema.pdv.caixa.CaixaService.FechamentoCaixa;
import com.empresax.sistema.pdv.caixa.CaixaService.ResumoSessaoCaixa;
import com.empresax.sistema.pdv.caixa.ContagemCedulas;
import com.empresax.sistema.shared.dinheiro.Dinheiro;
import com.empresax.sistema.usuario.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Caixa do operador (abrir, repor troco, sangria, fechar) e conferência de todos os caixas.
 * O operador só mexe no próprio caixa: o e-mail vem do login, nunca da requisição.
 */
@RestController
@RequestMapping("/api/caixa")
public class CaixaController {

    private final CaixaService caixaService;
    private final UsuarioService usuarioService;

    public CaixaController(CaixaService caixaService, UsuarioService usuarioService) {
        this.caixaService = caixaService;
        this.usuarioService = usuarioService;
    }

    /** 204 = o operador ainda não abriu o caixa. */
    @PreAuthorize(RegraAcesso.PDV_VENDER)
    @GetMapping("/atual")
    public ResponseEntity<CaixaAbertoResponse> atual(@AuthenticationPrincipal UserDetails operador) {
        return caixaService.caixaAberto(operador.getUsername())
                .map(sessao -> ResponseEntity.ok(CaixaAbertoResponse.de(sessao)))
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PreAuthorize(RegraAcesso.PDV_VENDER)
    @PostMapping("/abrir")
    @ResponseStatus(HttpStatus.CREATED)
    public CaixaAbertoResponse abrir(@Valid @RequestBody CedulasRequest requisicao, @AuthenticationPrincipal UserDetails operador) {
        return CaixaAbertoResponse.de(caixaService.abrir(operador.getUsername(), new ContagemCedulas(requisicao.cedulas())));
    }

    @PreAuthorize(RegraAcesso.PDV_VENDER)
    @PostMapping("/suprimento")
    public CaixaAbertoResponse registrarSuprimento(
            @Valid @RequestBody SuprimentoRequest requisicao, @AuthenticationPrincipal UserDetails operador
    ) {
        return CaixaAbertoResponse.de(caixaService.registrarSuprimento(
                operador.getUsername(), new ContagemCedulas(requisicao.cedulas()), requisicao.motivo()));
    }

    @PreAuthorize(RegraAcesso.PDV_VENDER)
    @PostMapping("/sangria")
    public CaixaAbertoResponse registrarSangria(
            @Valid @RequestBody SangriaRequest requisicao, @AuthenticationPrincipal UserDetails operador
    ) {
        return CaixaAbertoResponse.de(caixaService.registrarSangria(
                operador.getUsername(), new Dinheiro(requisicao.valor()), requisicao.motivo()));
    }

    /** O resultado (esperado × contado) só aparece depois de a contagem ser enviada. */
    @PreAuthorize(RegraAcesso.PDV_VENDER)
    @PostMapping("/fechar")
    public ConferenciaCaixaResponse fechar(@Valid @RequestBody FechamentoRequest requisicao, @AuthenticationPrincipal UserDetails operador) {
        FechamentoCaixa fechamento = caixaService.fechar(
                operador.getUsername(), new ContagemCedulas(requisicao.cedulas()), requisicao.observacao());
        return ConferenciaCaixaResponse.de(fechamento, usuarioService.nomesPorEmail(List.of(operador.getUsername())));
    }

    @PreAuthorize(RegraAcesso.PDV_VENDER + " or " + RegraAcesso.CAIXA_CONFERIR)
    @GetMapping("/fundo-padrao")
    public List<CedulaContadaResponse> fundoDeTrocoPadrao() {
        return CedulaContadaResponse.de(caixaService.fundoDeTrocoPadrao());
    }

    @PreAuthorize(RegraAcesso.CAIXA_CONFERIR)
    @PutMapping("/fundo-padrao")
    public List<CedulaContadaResponse> definirFundoDeTrocoPadrao(@Valid @RequestBody CedulasRequest requisicao) {
        return CedulaContadaResponse.de(caixaService.definirFundoDeTrocoPadrao(new ContagemCedulas(requisicao.cedulas())));
    }

    @PreAuthorize(RegraAcesso.CAIXA_CONFERIR)
    @GetMapping("/conferencia")
    public List<ResumoCaixaResponse> listarParaConferencia() {
        List<ResumoSessaoCaixa> resumos = caixaService.listarParaConferencia();
        Map<String, String> nomes = usuarioService.nomesPorEmail(
                resumos.stream().map(resumo -> resumo.sessao().operador()).collect(Collectors.toSet()));
        return resumos.stream().map(resumo -> ResumoCaixaResponse.de(resumo, nomes)).toList();
    }

    @PreAuthorize(RegraAcesso.CAIXA_CONFERIR)
    @GetMapping("/conferencia/{sessaoId}")
    public ConferenciaCaixaResponse detalharParaConferencia(@PathVariable UUID sessaoId) {
        FechamentoCaixa fechamento = caixaService.detalharParaConferencia(sessaoId);
        return ConferenciaCaixaResponse.de(fechamento, usuarioService.nomesPorEmail(List.of(fechamento.sessao().operador())));
    }
}

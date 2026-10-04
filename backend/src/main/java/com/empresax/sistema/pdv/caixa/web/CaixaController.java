package com.empresax.sistema.pdv.caixa.web;

import com.empresax.sistema.acesso.RegraAcesso;
import com.empresax.sistema.pdv.caixa.CaixaService;
import com.empresax.sistema.pdv.caixa.CaixaService.FechamentoCaixa;
import com.empresax.sistema.pdv.caixa.CaixaService.ResumoSessaoCaixa;
import com.empresax.sistema.pdv.caixa.ContagemCedulas;
import com.empresax.sistema.pdv.caixa.SessaoCaixa;
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

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Caixa separado da venda (D27):
 * - o operador (PDV_VENDER) só consulta se o próprio caixa está aberto;
 * - quem tem CAIXA_GERENCIAR abre, repõe troco, faz sangria e fecha qualquer caixa;
 * - quem tem CAIXA_CONFERIR vê a conferência de todos e define o fundo de troco padrão.
 * Quem fez cada operação vem do login, nunca da requisição.
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

    /** 204 = o caixa deste operador ainda não foi aberto. */
    @PreAuthorize(RegraAcesso.PDV_VENDER)
    @GetMapping("/atual")
    public ResponseEntity<CaixaAbertoResponse> atual(@AuthenticationPrincipal UserDetails operador) {
        return caixaService.caixaAberto(operador.getUsername())
                .map(sessao -> ResponseEntity.ok(responder(sessao)))
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PreAuthorize(RegraAcesso.CAIXA_GERENCIAR)
    @GetMapping("/abertos")
    public List<CaixaAbertoResponse> abertos() {
        List<SessaoCaixa> abertos = caixaService.listarAbertos();
        Map<String, String> nomes = nomes(abertos.stream().flatMap(sessao -> Stream.of(sessao.operador(), sessao.abertaPor())).toList());
        return abertos.stream().map(sessao -> CaixaAbertoResponse.de(sessao, nomes)).toList();
    }

    @PreAuthorize(RegraAcesso.CAIXA_GERENCIAR)
    @GetMapping("/operadores")
    public List<OperadorCaixaResponse> operadores() {
        return caixaService.operadores().stream().map(OperadorCaixaResponse::de).toList();
    }

    @PreAuthorize(RegraAcesso.CAIXA_GERENCIAR)
    @PostMapping("/abrir")
    @ResponseStatus(HttpStatus.CREATED)
    public CaixaAbertoResponse abrir(@Valid @RequestBody AberturaCaixaRequest requisicao, @AuthenticationPrincipal UserDetails responsavel) {
        return responder(caixaService.abrir(
                requisicao.operadorId(), new ContagemCedulas(requisicao.cedulas()), responsavel.getUsername()));
    }

    @PreAuthorize(RegraAcesso.CAIXA_GERENCIAR)
    @PostMapping("/{sessaoId}/suprimento")
    public CaixaAbertoResponse registrarSuprimento(
            @PathVariable UUID sessaoId, @Valid @RequestBody SuprimentoRequest requisicao, @AuthenticationPrincipal UserDetails responsavel
    ) {
        return responder(caixaService.registrarSuprimento(
                sessaoId, new ContagemCedulas(requisicao.cedulas()), requisicao.motivo(), responsavel.getUsername()));
    }

    @PreAuthorize(RegraAcesso.CAIXA_GERENCIAR)
    @PostMapping("/{sessaoId}/sangria")
    public CaixaAbertoResponse registrarSangria(
            @PathVariable UUID sessaoId, @Valid @RequestBody SangriaRequest requisicao, @AuthenticationPrincipal UserDetails responsavel
    ) {
        return responder(caixaService.registrarSangria(
                sessaoId, new Dinheiro(requisicao.valor()), requisicao.motivo(), responsavel.getUsername()));
    }

    /** O resultado (esperado × contado) só aparece depois de a contagem ser enviada. */
    @PreAuthorize(RegraAcesso.CAIXA_GERENCIAR)
    @PostMapping("/{sessaoId}/fechar")
    public ConferenciaCaixaResponse fechar(
            @PathVariable UUID sessaoId, @Valid @RequestBody FechamentoRequest requisicao, @AuthenticationPrincipal UserDetails responsavel
    ) {
        FechamentoCaixa fechamento = caixaService.fechar(
                sessaoId, new ContagemCedulas(requisicao.cedulas()), requisicao.observacao(), responsavel.getUsername());
        return conferencia(fechamento);
    }

    @PreAuthorize(RegraAcesso.CAIXA_GERENCIAR + " or " + RegraAcesso.CAIXA_CONFERIR)
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
        Map<String, String> nomes = nomes(resumos.stream().map(resumo -> resumo.sessao().operador()).toList());
        return resumos.stream().map(resumo -> ResumoCaixaResponse.de(resumo, nomes)).toList();
    }

    @PreAuthorize(RegraAcesso.CAIXA_CONFERIR)
    @GetMapping("/conferencia/{sessaoId}")
    public ConferenciaCaixaResponse detalharParaConferencia(@PathVariable UUID sessaoId) {
        return conferencia(caixaService.detalharParaConferencia(sessaoId));
    }

    private CaixaAbertoResponse responder(SessaoCaixa sessao) {
        return CaixaAbertoResponse.de(sessao, nomes(List.of(sessao.operador(), sessao.abertaPor())));
    }

    private ConferenciaCaixaResponse conferencia(FechamentoCaixa fechamento) {
        SessaoCaixa sessao = fechamento.sessao();
        List<String> emails = Stream.concat(Stream.of(sessao.operador(), sessao.abertaPor()), sessao.fechadaPor().stream()).toList();
        return ConferenciaCaixaResponse.de(fechamento, nomes(emails));
    }

    /** Nome de quem vendeu/abriu/fechou, resolvido numa consulta só (a sessão guarda o e-mail). */
    private Map<String, String> nomes(Collection<String> emails) {
        return usuarioService.nomesPorEmail(emails.stream().collect(Collectors.toSet()));
    }
}

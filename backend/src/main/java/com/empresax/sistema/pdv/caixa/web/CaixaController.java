package com.empresax.sistema.pdv.caixa.web;

import com.empresax.sistema.acesso.RegraAcesso;
import com.empresax.sistema.pdv.FormaPagamentoPresencial;
import com.empresax.sistema.pdv.caixa.CaixaService;
import com.empresax.sistema.pdv.caixa.CaixaService.AberturaDeCaixa;
import com.empresax.sistema.pdv.caixa.CaixaService.FechamentoCaixa;
import com.empresax.sistema.pdv.caixa.CaixaService.ResumoSessaoCaixa;
import com.empresax.sistema.pdv.caixa.ContagemCedulas;
import com.empresax.sistema.pdv.caixa.PontoCaixa;
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
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Caixa separado da venda (D27/D29):
 * - o operador (PDV_VENDER) só consulta se o próprio caixa está aberto;
 * - quem tem CAIXA_GERENCIAR cadastra os caixas numerados, abre (caixa + operador), repõe troco,
 *   faz sangria e fecha qualquer caixa;
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
                .map(sessao -> ResponseEntity.ok(CaixaAbertoResponse.de(sessao, nomes(List.of(sessao)))))
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PreAuthorize(RegraAcesso.CAIXA_GERENCIAR + " or " + RegraAcesso.CAIXA_CONFERIR)
    @GetMapping("/pontos")
    public List<PontoCaixaResponse> pontos() {
        Set<UUID> abertos = caixaService.pontosAbertos();
        return caixaService.listarPontos().stream().map(ponto -> PontoCaixaResponse.de(ponto, abertos)).toList();
    }

    @PreAuthorize(RegraAcesso.CAIXA_GERENCIAR)
    @PostMapping("/pontos")
    @ResponseStatus(HttpStatus.CREATED)
    public PontoCaixaResponse cadastrarPonto(@Valid @RequestBody PontoCaixaRequest requisicao) {
        return PontoCaixaResponse.de(caixaService.cadastrarPonto(requisicao.numero()), Set.of());
    }

    @PreAuthorize(RegraAcesso.CAIXA_GERENCIAR)
    @PostMapping("/pontos/{pontoId}/desativar")
    public PontoCaixaResponse desativarPonto(@PathVariable UUID pontoId) {
        return PontoCaixaResponse.de(caixaService.definirPontoAtivo(pontoId, false), caixaService.pontosAbertos());
    }

    @PreAuthorize(RegraAcesso.CAIXA_GERENCIAR)
    @PostMapping("/pontos/{pontoId}/ativar")
    public PontoCaixaResponse ativarPonto(@PathVariable UUID pontoId) {
        return PontoCaixaResponse.de(caixaService.definirPontoAtivo(pontoId, true), caixaService.pontosAbertos());
    }

    @PreAuthorize(RegraAcesso.CAIXA_GERENCIAR)
    @GetMapping("/abertos")
    public List<CaixaAbertoResponse> abertos() {
        List<SessaoCaixa> abertos = caixaService.listarAbertos();
        NomesDoCaixa nomes = nomes(abertos);
        return abertos.stream().map(sessao -> CaixaAbertoResponse.de(sessao, nomes)).toList();
    }

    @PreAuthorize(RegraAcesso.CAIXA_GERENCIAR)
    @GetMapping("/operadores")
    public List<OperadorCaixaResponse> operadores() {
        return caixaService.operadores().stream().map(OperadorCaixaResponse::de).toList();
    }

    /** Abre 1 ou mais caixas de uma vez: cada um com o seu operador e o mesmo fundo de troco. */
    @PreAuthorize(RegraAcesso.CAIXA_GERENCIAR)
    @PostMapping("/abrir")
    @ResponseStatus(HttpStatus.CREATED)
    public List<CaixaAbertoResponse> abrir(@Valid @RequestBody AberturaCaixaRequest requisicao, @AuthenticationPrincipal UserDetails responsavel) {
        List<AberturaDeCaixa> aberturas = requisicao.caixas().stream()
                .map(caixa -> new AberturaDeCaixa(caixa.pontoCaixaId(), caixa.operadorId()))
                .toList();
        List<SessaoCaixa> abertos = caixaService.abrir(aberturas, new ContagemCedulas(requisicao.cedulas()), responsavel.getUsername());
        NomesDoCaixa nomes = nomes(abertos);
        return abertos.stream().map(sessao -> CaixaAbertoResponse.de(sessao, nomes)).toList();
    }

    @PreAuthorize(RegraAcesso.CAIXA_GERENCIAR)
    @PostMapping("/{sessaoId}/suprimento")
    public CaixaAbertoResponse registrarSuprimento(
            @PathVariable UUID sessaoId, @Valid @RequestBody SuprimentoRequest requisicao, @AuthenticationPrincipal UserDetails responsavel
    ) {
        SessaoCaixa sessao = caixaService.registrarSuprimento(
                sessaoId, new ContagemCedulas(requisicao.cedulas()), requisicao.motivo(), responsavel.getUsername());
        return CaixaAbertoResponse.de(sessao, nomes(List.of(sessao)));
    }

    @PreAuthorize(RegraAcesso.CAIXA_GERENCIAR)
    @PostMapping("/{sessaoId}/sangria")
    public CaixaAbertoResponse registrarSangria(
            @PathVariable UUID sessaoId, @Valid @RequestBody SangriaRequest requisicao, @AuthenticationPrincipal UserDetails responsavel
    ) {
        SessaoCaixa sessao = caixaService.registrarSangria(
                sessaoId, new Dinheiro(requisicao.valor()), requisicao.motivo(), responsavel.getUsername());
        return CaixaAbertoResponse.de(sessao, nomes(List.of(sessao)));
    }

    /** O resultado (esperado × contado, sistema × maquininha) só aparece depois da contagem enviada. */
    @PreAuthorize(RegraAcesso.CAIXA_GERENCIAR)
    @PostMapping("/{sessaoId}/fechar")
    public ConferenciaCaixaResponse fechar(
            @PathVariable UUID sessaoId, @Valid @RequestBody FechamentoRequest requisicao, @AuthenticationPrincipal UserDetails responsavel
    ) {
        Map<FormaPagamentoPresencial, Dinheiro> maquininha = requisicao.maquininha().entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entrada -> new Dinheiro(entrada.getValue())));
        FechamentoCaixa fechamento = caixaService.fechar(
                sessaoId, new ContagemCedulas(requisicao.cedulas()), maquininha, requisicao.observacao(), responsavel.getUsername());
        return ConferenciaCaixaResponse.de(fechamento, nomes(List.of(fechamento.sessao())));
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
        NomesDoCaixa nomes = nomes(resumos.stream().map(ResumoSessaoCaixa::sessao).toList());
        return resumos.stream().map(resumo -> ResumoCaixaResponse.de(resumo, nomes)).toList();
    }

    @PreAuthorize(RegraAcesso.CAIXA_CONFERIR)
    @GetMapping("/conferencia/{sessaoId}")
    public ConferenciaCaixaResponse detalharParaConferencia(@PathVariable UUID sessaoId) {
        FechamentoCaixa fechamento = caixaService.detalharParaConferencia(sessaoId);
        return ConferenciaCaixaResponse.de(fechamento, nomes(List.of(fechamento.sessao())));
    }

    /** Nomes de quem vendeu/abriu/fechou e dos caixas, numa consulta de cada (a sessão guarda e-mail e id). */
    private NomesDoCaixa nomes(Collection<SessaoCaixa> sessoes) {
        Set<String> emails = sessoes.stream()
                .flatMap(sessao -> Stream.concat(Stream.of(sessao.operador(), sessao.abertaPor()), sessao.fechadaPor().stream()))
                .collect(Collectors.toSet());
        Map<UUID, String> caixas = caixaService.listarPontos().stream().collect(Collectors.toMap(PontoCaixa::id, PontoCaixa::nome));
        return new NomesDoCaixa(usuarioService.nomesPorEmail(emails), caixas);
    }
}

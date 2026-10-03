package com.empresax.sistema.atendimento.web;

import com.empresax.sistema.acesso.RegraAcesso;
import org.springframework.security.access.prepost.PreAuthorize;
import com.empresax.sistema.atendimento.conversa.ConversaService;
import com.empresax.sistema.atendimento.mensagem.MensagemService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Caixa de entrada (RF07): conversas atendidas pelo bot ou por atendentes humanos. Tags e SLA (também
 * citados em RF07) ainda não foram implementados.
 */
@RestController
@RequestMapping("/api/conversas")
public class ConversaController {

    private final ConversaService conversaService;
    private final MensagemService mensagemService;

    public ConversaController(ConversaService conversaService, MensagemService mensagemService) {
        this.conversaService = conversaService;
        this.mensagemService = mensagemService;
    }

    @PreAuthorize(RegraAcesso.CONVERSAS_VER)
    @GetMapping
    public List<ConversaResponse> listarAbertas() {
        return conversaService.listarAbertas().stream()
                .map(ConversaResponse::de)
                .toList();
    }

    @PreAuthorize(RegraAcesso.CONVERSAS_VER)
    @GetMapping("/{id}")
    public ConversaResponse buscarPorId(@PathVariable UUID id) {
        return ConversaResponse.de(conversaService.buscarPorId(id));
    }

    @PreAuthorize(RegraAcesso.CONVERSAS_ATENDER)
    @PutMapping("/{id}/atendente")
    public ConversaResponse atribuirAtendente(@PathVariable UUID id, @Valid @RequestBody AtribuirAtendenteRequest requisicao) {
        return ConversaResponse.de(conversaService.atribuirAtendente(id, requisicao.atendenteId()));
    }

    @PreAuthorize(RegraAcesso.CONVERSAS_ATENDER)
    @PostMapping("/{id}/assumir")
    public ConversaResponse assumir(@PathVariable UUID id, @AuthenticationPrincipal UserDetails usuario) {
        return ConversaResponse.de(conversaService.assumir(id, usuario.getUsername()));
    }

    @PreAuthorize(RegraAcesso.CONVERSAS_ATENDER)
    @PostMapping("/{id}/devolver-ao-bot")
    public ConversaResponse devolverAoBot(@PathVariable UUID id) {
        return ConversaResponse.de(conversaService.devolverAoBot(id));
    }

    @PreAuthorize(RegraAcesso.CONVERSAS_ATENDER)
    @PostMapping("/{id}/encerrar")
    public ConversaResponse encerrar(@PathVariable UUID id) {
        return ConversaResponse.de(conversaService.encerrar(id));
    }

    @PreAuthorize(RegraAcesso.CONVERSAS_VER)
    @GetMapping("/{id}/mensagens")
    public List<MensagemResponse> listarMensagens(@PathVariable UUID id) {
        return mensagemService.listarPorConversa(id).stream()
                .map(MensagemResponse::de)
                .toList();
    }

    @PreAuthorize(RegraAcesso.CONVERSAS_ATENDER)
    @PostMapping("/{id}/mensagens")
    public MensagemResponse enviarMensagem(
            @PathVariable UUID id,
            @Valid @RequestBody EnviarMensagemRequest requisicao,
            @AuthenticationPrincipal UserDetails usuario
    ) {
        return MensagemResponse.de(mensagemService.enviarPorAtendente(id, requisicao.conteudo(), usuario.getUsername()));
    }
}

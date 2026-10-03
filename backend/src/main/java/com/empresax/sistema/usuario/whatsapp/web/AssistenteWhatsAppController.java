package com.empresax.sistema.usuario.whatsapp.web;

import com.empresax.sistema.acesso.RegraAcesso;
import com.empresax.sistema.usuario.web.SessaoResponse;
import com.empresax.sistema.usuario.whatsapp.VinculoWhatsAppService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Cada pessoa liga o próprio WhatsApp ao assistente — nunca o de outra (exige o código enviado ao número). */
@RestController
@RequestMapping("/api/me/whatsapp")
public class AssistenteWhatsAppController {

    private final VinculoWhatsAppService vinculoWhatsAppService;

    public AssistenteWhatsAppController(VinculoWhatsAppService vinculoWhatsAppService) {
        this.vinculoWhatsAppService = vinculoWhatsAppService;
    }

    @PostMapping("/codigo")
    @PreAuthorize(RegraAcesso.ASSISTENTE_GESTOR_USAR)
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void enviarCodigo(
            @Valid @RequestBody EnviarCodigoWhatsAppRequest requisicao,
            @AuthenticationPrincipal UserDetails usuario
    ) {
        vinculoWhatsAppService.enviarCodigo(usuario.getUsername(), requisicao.telefone());
    }

    @PostMapping("/confirmar")
    @PreAuthorize(RegraAcesso.ASSISTENTE_GESTOR_USAR)
    public SessaoResponse confirmar(
            @Valid @RequestBody ConfirmarCodigoWhatsAppRequest requisicao,
            @AuthenticationPrincipal UserDetails usuario
    ) {
        return SessaoResponse.de(vinculoWhatsAppService.confirmar(usuario.getUsername(), requisicao.codigo()));
    }

    /** Desvincular não exige permissão: quem perdeu o acesso ao assistente ainda pode remover o número. */
    @DeleteMapping
    public SessaoResponse desvincular(@AuthenticationPrincipal UserDetails usuario) {
        return SessaoResponse.de(vinculoWhatsAppService.desvincular(usuario.getUsername()));
    }
}

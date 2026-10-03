package com.empresax.sistema.assistentegestor.web;

import com.empresax.sistema.acesso.RegraAcesso;
import com.empresax.sistema.assistentegestor.AssistenteGestorService;
import com.empresax.sistema.usuario.Usuario;
import com.empresax.sistema.usuario.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** Chat do assistente do gestor dentro do painel. */
@RestController
public class AssistenteController {

    private final AssistenteGestorService assistenteGestorService;
    private final UsuarioService usuarioService;

    public AssistenteController(AssistenteGestorService assistenteGestorService, UsuarioService usuarioService) {
        this.assistenteGestorService = assistenteGestorService;
        this.usuarioService = usuarioService;
    }

    @PostMapping("/api/assistente/perguntas")
    @PreAuthorize(RegraAcesso.ASSISTENTE_GESTOR_USAR)
    public RespostaAssistente perguntar(
            @Valid @RequestBody PerguntaAssistente requisicao,
            @AuthenticationPrincipal UserDetails autenticado
    ) {
        Usuario usuario = usuarioService.buscarComPermissoes(autenticado.getUsername());
        String resposta = assistenteGestorService.responder(requisicao.pergunta(), usuario.permissoes(), usuario.email());
        return new RespostaAssistente(resposta);
    }
}

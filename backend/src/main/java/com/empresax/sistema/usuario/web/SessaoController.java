package com.empresax.sistema.usuario.web;

import com.empresax.sistema.usuario.UsuarioService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Qualquer usuário autenticado consulta a própria sessão (sem permissão específica). */
@RestController
public class SessaoController {

    private final UsuarioService usuarioService;

    public SessaoController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping("/api/me")
    public SessaoResponse sessaoAtual(@AuthenticationPrincipal UserDetails usuario) {
        return SessaoResponse.de(usuarioService.buscarComPermissoes(usuario.getUsername()));
    }
}

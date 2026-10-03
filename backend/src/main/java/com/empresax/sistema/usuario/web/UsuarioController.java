package com.empresax.sistema.usuario.web;

import com.empresax.sistema.acesso.RegraAcesso;
import com.empresax.sistema.usuario.NovoUsuario;
import com.empresax.sistema.usuario.Usuario;
import com.empresax.sistema.usuario.UsuarioService;
import jakarta.validation.Valid;
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
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/usuarios")
@PreAuthorize(RegraAcesso.USUARIOS_GERENCIAR)
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping
    public List<UsuarioResponse> listar() {
        return usuarioService.listarTodos().stream()
                .map(UsuarioResponse::de)
                .toList();
    }

    @PostMapping
    public ResponseEntity<UsuarioResponse> cadastrar(
            @Valid @RequestBody CriarUsuarioRequest requisicao,
            @AuthenticationPrincipal UserDetails ator
    ) {
        Usuario usuario = usuarioService.cadastrar(ator.getUsername(), new NovoUsuario(
                requisicao.nome(), requisicao.email(), requisicao.senha(),
                requisicao.acessoIrrestrito(), requisicao.cargoId()));
        return ResponseEntity.created(URI.create("/api/usuarios/" + usuario.id())).body(UsuarioResponse.de(usuario));
    }

    @PutMapping("/{id}/cargo")
    public UsuarioResponse trocarCargo(
            @PathVariable UUID id,
            @Valid @RequestBody TrocarCargoRequest requisicao,
            @AuthenticationPrincipal UserDetails ator
    ) {
        return UsuarioResponse.de(usuarioService.trocarCargo(ator.getUsername(), id, requisicao.cargoId()));
    }

    @PostMapping("/{id}/desativar")
    public UsuarioResponse desativar(@PathVariable UUID id, @AuthenticationPrincipal UserDetails ator) {
        return UsuarioResponse.de(usuarioService.desativar(ator.getUsername(), id));
    }

    @PostMapping("/{id}/ativar")
    public UsuarioResponse ativar(@PathVariable UUID id, @AuthenticationPrincipal UserDetails ator) {
        return UsuarioResponse.de(usuarioService.ativar(ator.getUsername(), id));
    }
}

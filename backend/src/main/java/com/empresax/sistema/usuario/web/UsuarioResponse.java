package com.empresax.sistema.usuario.web;

import com.empresax.sistema.acesso.Cargo;
import com.empresax.sistema.usuario.Usuario;

import java.util.UUID;

public record UsuarioResponse(
        UUID id,
        String nome,
        String email,
        boolean acessoIrrestrito,
        UUID cargoId,
        String cargoNome,
        boolean ativo
) {

    public static UsuarioResponse de(Usuario usuario) {
        return new UsuarioResponse(
                usuario.id(),
                usuario.nome(),
                usuario.email(),
                usuario.acessoIrrestrito(),
                usuario.cargo().map(Cargo::id).orElse(null),
                usuario.cargo().map(Cargo::nome).orElse(null),
                usuario.ativo());
    }
}

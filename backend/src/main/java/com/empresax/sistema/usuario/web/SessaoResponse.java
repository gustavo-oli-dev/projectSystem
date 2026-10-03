package com.empresax.sistema.usuario.web;

import com.empresax.sistema.acesso.Cargo;
import com.empresax.sistema.acesso.Permissao;
import com.empresax.sistema.usuario.Usuario;

import java.util.List;

/** Quem está logado e o que pode fazer — o frontend usa para mostrar/esconder áreas e botões. */
public record SessaoResponse(
        String nome,
        String email,
        boolean acessoIrrestrito,
        String cargo,
        List<String> permissoes,
        String telefoneWhatsapp
) {

    public static SessaoResponse de(Usuario usuario) {
        return new SessaoResponse(
                usuario.nome(),
                usuario.email(),
                usuario.acessoIrrestrito(),
                usuario.cargo().map(Cargo::nome).orElse(null),
                usuario.permissoes().stream().map(Permissao::name).sorted().toList(),
                usuario.telefoneWhatsapp().orElse(null));
    }
}

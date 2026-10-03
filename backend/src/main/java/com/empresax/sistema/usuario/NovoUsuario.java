package com.empresax.sistema.usuario;

import java.util.UUID;

/** Dados para cadastrar um usuário: ou acesso irrestrito, ou um cargo. */
public record NovoUsuario(String nome, String email, String senha, boolean acessoIrrestrito, UUID cargoId) {
}

package com.empresax.sistema.usuario.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CriarUsuarioRequest(
        @NotBlank(message = "Nome é obrigatório") String nome,
        @NotBlank(message = "E-mail é obrigatório")
        @Email(message = "E-mail inválido") String email,
        @NotBlank(message = "Senha é obrigatória")
        @Size(min = 8, message = "Senha deve ter ao menos 8 caracteres") String senha,
        boolean acessoIrrestrito,
        UUID cargoId
) {
}

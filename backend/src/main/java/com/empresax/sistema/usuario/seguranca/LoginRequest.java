package com.empresax.sistema.usuario.seguranca;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "E-mail é obrigatório") String email,
        @NotBlank(message = "Senha é obrigatória") String senha
) {
}

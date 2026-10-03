package com.empresax.sistema.cliente.web;

import jakarta.validation.constraints.NotBlank;

public record AtualizarClienteRequest(
        @NotBlank(message = "Nome é obrigatório") String nome,
        @NotBlank(message = "Telefone do WhatsApp é obrigatório") String telefoneWhatsapp
) {
}

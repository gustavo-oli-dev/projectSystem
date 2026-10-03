package com.empresax.sistema.cliente.web;

import jakarta.validation.constraints.NotBlank;

public record CriarClienteRequest(
        @NotBlank(message = "Nome é obrigatório") String nome,
        @NotBlank(message = "Documento (CPF ou CNPJ) é obrigatório") String documento,
        @NotBlank(message = "Telefone do WhatsApp é obrigatório") String telefoneWhatsapp
) {
}

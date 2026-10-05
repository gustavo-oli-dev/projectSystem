package com.empresax.sistema.compras.contato.web;

import com.empresax.sistema.compras.contato.TipoContato;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ContatoRequest(
        @NotNull(message = "Informe o tipo do contato") TipoContato tipo,
        @NotBlank(message = "Informe o nome do contato") @Size(max = 150, message = "Nome muito longo") String nome,
        @Size(max = 18, message = "CNPJ/CPF inválido") String documento,
        @Size(max = 30, message = "Telefone muito longo") String telefone,
        @Size(max = 150, message = "E-mail muito longo") String email,
        @Size(max = 500, message = "Observação muito longa") String observacao
) {
}

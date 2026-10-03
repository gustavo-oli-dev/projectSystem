package com.empresax.sistema.atendimento.web;

import jakarta.validation.constraints.NotBlank;

public record EnviarMensagemRequest(
        @NotBlank(message = "Conteúdo da mensagem é obrigatório") String conteudo
) {
}

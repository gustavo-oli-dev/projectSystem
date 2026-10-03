package com.empresax.sistema.assistentegestor.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PerguntaAssistente(
        @NotBlank(message = "Escreva uma pergunta")
        @Size(max = 1000, message = "Pergunta deve ter no máximo 1000 caracteres") String pergunta
) {
}

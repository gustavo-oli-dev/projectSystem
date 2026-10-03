package com.empresax.sistema.atendimento.web;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AtribuirAtendenteRequest(
        @NotNull(message = "Atendente é obrigatório") UUID atendenteId
) {
}

package com.empresax.sistema.usuario.web;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record TrocarCargoRequest(@NotNull(message = "Perfil de acesso é obrigatório") UUID cargoId) {
}

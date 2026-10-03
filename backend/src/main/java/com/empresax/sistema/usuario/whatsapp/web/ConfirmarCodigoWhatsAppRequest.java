package com.empresax.sistema.usuario.whatsapp.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ConfirmarCodigoWhatsAppRequest(
        @NotBlank @Pattern(regexp = "\\d{6}", message = "O código tem 6 números") String codigo
) {
}

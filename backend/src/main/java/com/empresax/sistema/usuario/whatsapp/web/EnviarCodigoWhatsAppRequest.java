package com.empresax.sistema.usuario.whatsapp.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EnviarCodigoWhatsAppRequest(@NotBlank @Size(max = 30) String telefone) {
}

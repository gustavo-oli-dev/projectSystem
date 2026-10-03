package com.empresax.sistema.cobranca.web;

import com.empresax.sistema.cobranca.MeioCobranca;
import jakarta.validation.constraints.NotNull;

public record CriarCobrancaRequest(
        @NotNull(message = "Meio de cobrança (PIX ou BOLETO) é obrigatório") MeioCobranca meio
) {
}

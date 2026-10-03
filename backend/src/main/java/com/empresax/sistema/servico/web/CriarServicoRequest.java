package com.empresax.sistema.servico.web;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CriarServicoRequest(
        @NotBlank(message = "Nome é obrigatório") String nome,
        String descricao,
        @NotBlank(message = "Código de serviço (LC 116/2003) é obrigatório") String codigoServicoLc116,
        @NotNull(message = "Alíquota de ISS é obrigatória") BigDecimal aliquotaIss,
        @NotNull(message = "Preço unitário é obrigatório")
        @DecimalMin(value = "0.0", message = "Preço unitário não pode ser negativo") BigDecimal precoUnitario
) {
}

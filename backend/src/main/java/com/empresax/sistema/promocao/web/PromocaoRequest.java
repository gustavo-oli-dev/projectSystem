package com.empresax.sistema.promocao.web;

import com.empresax.sistema.promocao.TipoPromocao;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** precoOferta só no PRECO_OFERTA; leve e pague só no LEVE_PAGUE. */
public record PromocaoRequest(
        @NotNull(message = "Escolha o produto") UUID produtoId,
        @NotNull(message = "Escolha o tipo da promoção") TipoPromocao tipo,
        @DecimalMin(value = "0.01", message = "O preço de oferta precisa ser maior que zero") BigDecimal precoOferta,
        @Min(value = 2, message = "Leve ao menos 2") @Max(value = 100, message = "Leve no máximo 100") Integer leve,
        @Min(value = 1, message = "Pague ao menos 1") Integer pague,
        @NotNull(message = "Informe o primeiro dia") LocalDate inicio,
        @NotNull(message = "Informe o último dia") LocalDate fim
) {
}

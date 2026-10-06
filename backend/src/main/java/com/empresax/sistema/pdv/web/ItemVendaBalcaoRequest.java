package com.empresax.sistema.pdv.web;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ItemVendaBalcaoRequest(
        @NotNull(message = "Produto é obrigatório") UUID produtoId,
        /** Vendido numa embalagem do produto (ex.: fardo com 12); vazio = unidade avulsa. */
        UUID embalagemId,
        @Min(value = 1, message = "Quantidade mínima é 1")
        @Max(value = 999, message = "Quantidade muito alta para uma venda de balcão") int quantidade
) {
}

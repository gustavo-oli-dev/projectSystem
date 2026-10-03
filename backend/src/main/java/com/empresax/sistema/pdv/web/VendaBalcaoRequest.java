package com.empresax.sistema.pdv.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record VendaBalcaoRequest(
        @NotEmpty(message = "A venda precisa de ao menos um produto") @Size(max = 100) List<@Valid ItemVendaBalcaoRequest> itens,
        @Size(max = 14, message = "CPF inválido") String cpfNaNota,
        @NotNull(message = "Informe o pagamento") @Valid PagamentoPresencialRequest pagamento
) {
}

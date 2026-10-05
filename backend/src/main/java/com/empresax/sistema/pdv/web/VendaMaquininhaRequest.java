package com.empresax.sistema.pdv.web;

import com.empresax.sistema.pdv.FormaPagamentoPresencial;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record VendaMaquininhaRequest(
        @NotEmpty(message = "A venda precisa de ao menos um produto") @Size(max = 100) List<@Valid ItemVendaBalcaoRequest> itens,
        @Size(max = 14, message = "CPF inválido") String cpfNaNota,
        UUID clienteId,
        @Valid DescontoVendaRequest desconto,
        @NotNull(message = "Escolha crédito ou débito") FormaPagamentoPresencial forma
) {
}

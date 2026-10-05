package com.empresax.sistema.pdv.web;

import com.empresax.sistema.pdv.PartesDoPagamento;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record VendaBalcaoRequest(
        @NotEmpty(message = "A venda precisa de ao menos um produto") @Size(max = 100) List<@Valid ItemVendaBalcaoRequest> itens,
        @Size(max = 14, message = "CPF inválido") String cpfNaNota,
        UUID clienteId,
        @Valid DescontoVendaRequest desconto,
        /** Pagamento dividido: partes já recebidas antes da forma que fecha a venda (D36). */
        @Size(max = PartesDoPagamento.MAXIMO_DE_PARTES, message = "No máximo 5 partes já recebidas")
        List<@Valid PagamentoPresencialRequest> partes,
        @NotNull(message = "Informe o pagamento") @Valid PagamentoPresencialRequest pagamento
) {
}

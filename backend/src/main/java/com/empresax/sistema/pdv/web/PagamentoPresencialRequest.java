package com.empresax.sistema.pdv.web;

import com.empresax.sistema.pdv.BandeiraCartao;
import com.empresax.sistema.pdv.FormaPagamentoPresencial;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record PagamentoPresencialRequest(
        @NotNull(message = "Escolha a forma de pagamento") FormaPagamentoPresencial forma,
        @DecimalMin(value = "0.0", message = "Valor recebido não pode ser negativo") BigDecimal valorRecebido,
        BandeiraCartao bandeira,
        @Size(max = 20, message = "Código de autorização tem no máximo 20 caracteres") String codigoAutorizacao
) {
}

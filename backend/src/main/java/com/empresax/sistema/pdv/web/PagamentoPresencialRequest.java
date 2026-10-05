package com.empresax.sistema.pdv.web;

import com.empresax.sistema.pdv.BandeiraCartao;
import com.empresax.sistema.pdv.DadosPagamentoPresencial;
import com.empresax.sistema.pdv.FormaPagamentoPresencial;
import com.empresax.sistema.pdv.PartesDoPagamento;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

/** valor: só nas partes do pagamento dividido (quanto esta parte paga); na forma final fica vazio. */
public record PagamentoPresencialRequest(
        @NotNull(message = "Escolha a forma de pagamento") FormaPagamentoPresencial forma,
        @DecimalMin(value = "0.01", message = "O valor da parte precisa ser maior que zero") BigDecimal valor,
        @DecimalMin(value = "0.0", message = "Valor recebido não pode ser negativo") BigDecimal valorRecebido,
        BandeiraCartao bandeira,
        @Size(max = 20, message = "Código de autorização tem no máximo 20 caracteres") String codigoAutorizacao
) {

    public DadosPagamentoPresencial paraDados() {
        return new DadosPagamentoPresencial(forma, valor, valorRecebido, bandeira, codigoAutorizacao);
    }

    /** Partes já recebidas do pagamento dividido (D36); sem partes = pagamento numa forma só. */
    public static PartesDoPagamento partes(List<PagamentoPresencialRequest> partes) {
        if (partes == null) {
            return PartesDoPagamento.nenhuma();
        }
        return new PartesDoPagamento(partes.stream().map(PagamentoPresencialRequest::paraDados).toList());
    }
}

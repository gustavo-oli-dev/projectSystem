package com.empresax.sistema.pdv.caixa.web;

import com.empresax.sistema.pdv.caixa.Cedula;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Abertura feita por quem gerencia o caixa: quais caixas, o operador de cada um e as cédulas de cada gaveta. */
public record AberturaCaixaRequest(
        @NotEmpty(message = "Escolha ao menos um caixa") @Size(max = 50, message = "Caixas demais de uma vez")
        List<@Valid @NotNull CaixaComOperador> caixas,
        @NotNull(message = "Informe a contagem das cédulas")
        Map<Cedula, @NotNull @PositiveOrZero @Max(value = CedulasRequest.QUANTIDADE_MAXIMA, message = "Quantidade de cédulas fora do limite") Integer> cedulas
) {

    public record CaixaComOperador(
            @NotNull(message = "Escolha o caixa") UUID pontoCaixaId,
            @NotNull(message = "Escolha o operador de cada caixa") UUID operadorId
    ) {
    }
}

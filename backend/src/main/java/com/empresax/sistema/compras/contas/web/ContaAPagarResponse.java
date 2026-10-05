package com.empresax.sistema.compras.contas.web;

import com.empresax.sistema.compras.contas.ContaAPagar;
import com.empresax.sistema.compras.contas.StatusContaAPagar;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

public record ContaAPagarResponse(
        UUID id, UUID contatoId, String contatoNome, String descricao, BigDecimal valor, LocalDate vencimento,
        StatusContaAPagar status, boolean vencida, boolean daNota, Instant pagaEm, String pagaPor
) {

    static ContaAPagarResponse de(ContaAPagar conta, Map<UUID, String> nomesDosContatos, LocalDate hoje) {
        return new ContaAPagarResponse(
                conta.id(),
                conta.contatoId().orElse(null),
                conta.contatoId().map(nomesDosContatos::get).orElse(null),
                conta.descricao(),
                conta.valor().valor(),
                conta.vencimento(),
                conta.status(),
                conta.vencida(hoje),
                conta.notaEntradaId().isPresent(),
                conta.pagaEm().orElse(null),
                conta.pagaPor().orElse(null));
    }
}

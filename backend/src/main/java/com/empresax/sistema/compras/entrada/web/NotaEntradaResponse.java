package com.empresax.sistema.compras.entrada.web;

import com.empresax.sistema.compras.entrada.NotaEntrada;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record NotaEntradaResponse(
        UUID id, String numero, String chaveAcesso, String fornecedorNome, BigDecimal valorTotal,
        int itens, int unidades, String registradaPor, Instant registradaEm
) {

    static NotaEntradaResponse de(NotaEntrada nota, Map<UUID, String> nomesDosContatos) {
        return new NotaEntradaResponse(
                nota.id(), nota.numero(), nota.chaveAcesso(), nomesDosContatos.getOrDefault(nota.fornecedorId(), "—"),
                nota.valorTotal().valor(), nota.itens().size(),
                nota.itens().stream().mapToInt(NotaEntrada.Item::quantidade).sum(),
                nota.registradaPor(), nota.registradaEm());
    }
}

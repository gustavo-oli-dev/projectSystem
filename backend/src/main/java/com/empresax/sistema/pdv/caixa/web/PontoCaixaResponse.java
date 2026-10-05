package com.empresax.sistema.pdv.caixa.web;

import com.empresax.sistema.pdv.caixa.PontoCaixa;

import java.util.Set;
import java.util.UUID;

/** Caixa físico da loja; "aberto" = tem uma abertura em andamento agora. */
public record PontoCaixaResponse(UUID id, int numero, String nome, boolean ativo, boolean aberto) {

    static PontoCaixaResponse de(PontoCaixa ponto, Set<UUID> abertos) {
        return new PontoCaixaResponse(ponto.id(), ponto.numero(), ponto.nome(), ponto.ativo(), abertos.contains(ponto.id()));
    }
}

package com.empresax.sistema.pdv.caixa.web;

import com.empresax.sistema.pdv.caixa.CaixaService.OperadorDeCaixa;

import java.util.UUID;

public record OperadorCaixaResponse(UUID id, String nome, String email, boolean caixaAberto) {

    static OperadorCaixaResponse de(OperadorDeCaixa operador) {
        return new OperadorCaixaResponse(operador.id(), operador.nome(), operador.email(), operador.caixaAberto());
    }
}

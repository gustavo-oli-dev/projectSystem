package com.empresax.sistema.compras.contato.web;

import com.empresax.sistema.compras.contato.Contato;
import com.empresax.sistema.compras.contato.TipoContato;

import java.util.UUID;

public record ContatoResponse(
        UUID id, TipoContato tipo, String nome, String documento, String telefone, String email, String observacao, boolean ativo
) {

    public static ContatoResponse de(Contato contato) {
        return new ContatoResponse(
                contato.id(), contato.tipo(), contato.nome(), contato.documento().orElse(null), contato.telefone().orElse(null),
                contato.email().orElse(null), contato.observacao().orElse(null), contato.ativo());
    }
}

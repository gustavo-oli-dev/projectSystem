package com.empresax.sistema.cliente.web;

import com.empresax.sistema.cliente.Cliente;

import java.time.Instant;
import java.util.UUID;

public record ClienteResponse(
        UUID id,
        String nome,
        String documento,
        String telefoneWhatsapp,
        Instant criadoEm
) {

    public static ClienteResponse de(Cliente cliente) {
        return new ClienteResponse(
                cliente.id(),
                cliente.nome(),
                cliente.documento().valor(),
                cliente.telefoneWhatsapp(),
                cliente.criadoEm()
        );
    }
}

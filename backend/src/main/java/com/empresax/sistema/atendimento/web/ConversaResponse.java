package com.empresax.sistema.atendimento.web;

import com.empresax.sistema.atendimento.conversa.Conversa;
import com.empresax.sistema.atendimento.conversa.ModoAtendimento;
import com.empresax.sistema.atendimento.conversa.StatusConversa;

import java.time.Instant;
import java.util.UUID;

public record ConversaResponse(
        UUID id,
        String telefoneWhatsapp,
        UUID clienteId,
        StatusConversa status,
        ModoAtendimento modo,
        UUID atendenteId,
        String motivoTransferencia,
        Instant criadaEm,
        Instant atualizadaEm
) {

    public static ConversaResponse de(Conversa conversa) {
        return new ConversaResponse(
                conversa.id(),
                conversa.telefoneWhatsapp(),
                conversa.clienteId(),
                conversa.status(),
                conversa.modo(),
                conversa.atendenteId(),
                conversa.motivoTransferencia(),
                conversa.criadaEm(),
                conversa.atualizadaEm());
    }
}

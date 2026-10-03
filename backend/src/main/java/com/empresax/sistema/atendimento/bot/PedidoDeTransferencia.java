package com.empresax.sistema.atendimento.bot;

import java.util.Optional;

/**
 * Registra, durante uma resposta do bot, se a IA pediu para transferir a conversa a um humano.
 * Mutável de propósito: é preenchido pela ferramenta no meio do loop e lido no fim.
 */
final class PedidoDeTransferencia {

    private String motivo;

    void registrar(String motivoInformado) {
        this.motivo = motivoInformado == null || motivoInformado.isBlank()
                ? "Cliente precisa de atendimento humano"
                : motivoInformado;
    }

    Optional<String> motivo() {
        return Optional.ofNullable(motivo);
    }
}

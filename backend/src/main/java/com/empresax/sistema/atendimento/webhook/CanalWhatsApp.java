package com.empresax.sistema.atendimento.webhook;

/** Em qual número a mensagem chegou (D17): cada canal tem instância, webhook e token próprios. */
public enum CanalWhatsApp {
    /** Número da empresa: só clientes, só o bot de atendimento. */
    ATENDIMENTO,
    /** Número interno: só funcionários verificados, só o assistente do gestor. */
    ASSISTENTE
}

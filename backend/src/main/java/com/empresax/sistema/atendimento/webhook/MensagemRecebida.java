package com.empresax.sistema.atendimento.webhook;

/** Mensagem de texto recebida, já extraída do payload do provedor de WhatsApp. */
public record MensagemRecebida(String telefoneRemetente, String texto, String idExterno) {
}

package com.empresax.sistema.atendimento.web;

import com.empresax.sistema.atendimento.mensagem.Mensagem;
import com.empresax.sistema.atendimento.mensagem.OrigemMensagem;

import java.time.Instant;
import java.util.UUID;

public record MensagemResponse(UUID id, OrigemMensagem origem, String conteudo, Instant enviadaEm) {

    public static MensagemResponse de(Mensagem mensagem) {
        return new MensagemResponse(mensagem.id(), mensagem.origem(), mensagem.conteudo(), mensagem.enviadaEm());
    }
}

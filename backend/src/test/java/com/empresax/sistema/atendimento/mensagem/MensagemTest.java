package com.empresax.sistema.atendimento.mensagem;

import com.empresax.sistema.common.domain.DomainException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MensagemTest {

    @Test
    void criaMensagemComOsDadosInformados() {
        UUID conversaId = UUID.randomUUID();

        Mensagem mensagem = new Mensagem(conversaId, OrigemMensagem.CLIENTE, "Olá", "wa-123");

        assertThat(mensagem.conversaId()).isEqualTo(conversaId);
        assertThat(mensagem.conteudo()).isEqualTo("Olá");
    }

    @Test
    void rejeitaMensagemSemConversa() {
        assertThatThrownBy(() -> new Mensagem(null, OrigemMensagem.CLIENTE, "Olá", null))
                .isInstanceOf(DomainException.class);
    }

    @Test
    void rejeitaMensagemSemOrigem() {
        assertThatThrownBy(() -> new Mensagem(UUID.randomUUID(), null, "Olá", null))
                .isInstanceOf(DomainException.class);
    }
}

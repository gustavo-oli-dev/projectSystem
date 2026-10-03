package com.empresax.sistema.cobranca.webhook;

import com.empresax.sistema.common.domain.DomainException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EventoWebhookPagamentoTest {

    @Test
    void nasceComStatusPendente() {
        EventoWebhookPagamento evento = new EventoWebhookPagamento("123456789");

        assertThat(evento.status()).isEqualTo(StatusEventoWebhook.PENDENTE);
    }

    @Test
    void marcarComoProcessadoRegistraDataDeProcessamento() {
        EventoWebhookPagamento evento = new EventoWebhookPagamento("123456789");

        evento.marcarComoProcessado();

        assertThat(evento.status()).isEqualTo(StatusEventoWebhook.PROCESSADO);
        assertThat(evento.processadoEm()).isNotNull();
    }

    @Test
    void rejeitaReferenciaExternaEmBranco() {
        assertThatThrownBy(() -> new EventoWebhookPagamento(" ")).isInstanceOf(DomainException.class);
    }
}

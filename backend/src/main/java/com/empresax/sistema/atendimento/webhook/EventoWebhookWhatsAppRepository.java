package com.empresax.sistema.atendimento.webhook;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface EventoWebhookWhatsAppRepository extends JpaRepository<EventoWebhookWhatsApp, UUID> {

    List<EventoWebhookWhatsApp> findByStatusOrderByRecebidoEmAsc(StatusEventoWebhookWhatsApp status);
}

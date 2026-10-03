package com.empresax.sistema.cobranca.webhook;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface EventoWebhookPagamentoRepository extends JpaRepository<EventoWebhookPagamento, UUID> {

    // Seleção simples: v1 roda com um único backend (decisão D5, host único). Se um dia escalar
    // para múltiplas instâncias, trocar por uma query nativa com FOR UPDATE SKIP LOCKED para
    // evitar que duas instâncias processem o mesmo evento ao mesmo tempo.
    List<EventoWebhookPagamento> findByStatusOrderByRecebidoEmAsc(StatusEventoWebhook status);
}

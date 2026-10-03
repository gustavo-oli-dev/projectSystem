package com.empresax.sistema.atendimento.webhook;

import com.empresax.sistema.common.domain.DomainException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Outbox do webhook da W-API (mesmo padrão do pagamento, decisão D4/RNF05 em
 * DECISOES.md): o controller só grava o payload bruto e responde rápido; o processor interpreta
 * de forma assíncrona.
 */
@Entity
@Table(name = "eventos_webhook_whatsapp")
public class EventoWebhookWhatsApp {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, columnDefinition = "text")
    private String payloadBruto;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private CanalWhatsApp canal;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusEventoWebhookWhatsApp status;

    @Column(nullable = false, updatable = false)
    private Instant recebidoEm;

    @Column
    private Instant processadoEm;

    protected EventoWebhookWhatsApp() {
        // exigido pelo JPA
    }

    public EventoWebhookWhatsApp(CanalWhatsApp canal, String payloadBruto) {
        if (canal == null) {
            throw new DomainException("Canal do evento de webhook é obrigatório");
        }
        if (payloadBruto == null || payloadBruto.isBlank()) {
            throw new DomainException("Payload do evento de webhook é obrigatório");
        }
        this.canal = canal;
        this.payloadBruto = payloadBruto;
        this.status = StatusEventoWebhookWhatsApp.PENDENTE;
        this.recebidoEm = Instant.now();
    }

    public void marcarComoProcessado() {
        this.status = StatusEventoWebhookWhatsApp.PROCESSADO;
        this.processadoEm = Instant.now();
    }

    public void marcarComoFalhou() {
        this.status = StatusEventoWebhookWhatsApp.FALHOU;
        this.processadoEm = Instant.now();
    }

    public UUID id() {
        return id;
    }

    public CanalWhatsApp canal() {
        return canal;
    }

    public String payloadBruto() {
        return payloadBruto;
    }

    public StatusEventoWebhookWhatsApp status() {
        return status;
    }

    public Instant recebidoEm() {
        return recebidoEm;
    }

    public Instant processadoEm() {
        return processadoEm;
    }
}

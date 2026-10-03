package com.empresax.sistema.cobranca.webhook;

import com.empresax.sistema.common.domain.DomainException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.UUID;

/**
 * Outbox do webhook de pagamento (RNF05 / decisão D4 em DECISOES.md): o controller só grava este
 * evento e responde rápido; o WebhookPagamentoProcessor processa de forma assíncrona, sempre
 * confirmando o status com uma consulta à API do provedor.
 */
@Entity
@Table(name = "eventos_webhook_pagamento", uniqueConstraints = @UniqueConstraint(columnNames = "referencia_externa"))
public class EventoWebhookPagamento {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String referenciaExterna;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusEventoWebhook status;

    @Column(nullable = false, updatable = false)
    private Instant recebidoEm;

    @Column
    private Instant processadoEm;

    protected EventoWebhookPagamento() {
        // exigido pelo JPA
    }

    public EventoWebhookPagamento(String referenciaExterna) {
        if (referenciaExterna == null || referenciaExterna.isBlank()) {
            throw new DomainException("Referência externa do evento de webhook é obrigatória");
        }
        this.referenciaExterna = referenciaExterna;
        this.status = StatusEventoWebhook.PENDENTE;
        this.recebidoEm = Instant.now();
    }

    public void marcarComoProcessado() {
        this.status = StatusEventoWebhook.PROCESSADO;
        this.processadoEm = Instant.now();
    }

    public void marcarComoFalhou() {
        this.status = StatusEventoWebhook.FALHOU;
        this.processadoEm = Instant.now();
    }

    public UUID id() {
        return id;
    }

    public String referenciaExterna() {
        return referenciaExterna;
    }

    public StatusEventoWebhook status() {
        return status;
    }

    public Instant recebidoEm() {
        return recebidoEm;
    }

    public Instant processadoEm() {
        return processadoEm;
    }
}

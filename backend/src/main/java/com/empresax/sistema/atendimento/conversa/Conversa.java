package com.empresax.sistema.atendimento.conversa;

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
 * Uma conversa é identificada pelo número de WhatsApp, não por um Cliente: a primeira mensagem
 * chega antes de qualquer cadastro existir (RF01). Quando já existe um Cliente com esse telefone,
 * a conversa nasce vinculada a ele; senão, fica como contato não identificado até alguém
 * formalizar o cadastro.
 */
@Entity
@Table(name = "conversas")
public class Conversa {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String telefoneWhatsapp;

    @Column
    private UUID clienteId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusConversa status;

    @Column
    private UUID atendenteId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ModoAtendimento modo;

    @Column
    private String motivoTransferencia;

    @Column(nullable = false, updatable = false)
    private Instant criadaEm;

    @Column(nullable = false)
    private Instant atualizadaEm;

    protected Conversa() {
        // exigido pelo JPA
    }

    public Conversa(String telefoneWhatsapp, UUID clienteId) {
        this.telefoneWhatsapp = validarTelefone(telefoneWhatsapp);
        this.clienteId = clienteId;
        this.status = StatusConversa.ABERTA;
        this.modo = ModoAtendimento.BOT;
        this.criadaEm = Instant.now();
        this.atualizadaEm = this.criadaEm;
    }

    private static String validarTelefone(String telefone) {
        if (telefone == null || telefone.isBlank()) {
            throw new DomainException("Telefone do WhatsApp da conversa é obrigatório");
        }
        return telefone.trim();
    }

    /** Um atendente assume a conversa: o bot para de responder. */
    public void atribuirAtendente(UUID atendenteId) {
        garantirAberta();
        if (atendenteId == null) {
            throw new DomainException("Atendente é obrigatório para atribuição");
        }
        this.atendenteId = atendenteId;
        this.modo = ModoAtendimento.HUMANO;
        this.motivoTransferencia = null;
        this.atualizadaEm = Instant.now();
    }

    /** O bot não consegue (ou não deve) seguir: a conversa vai pra fila humana, sem atendente ainda. */
    public void transferirParaAtendente(String motivo) {
        garantirAberta();
        if (motivo == null || motivo.isBlank()) {
            throw new DomainException("Motivo da transferência para atendente é obrigatório");
        }
        this.modo = ModoAtendimento.HUMANO;
        this.motivoTransferencia = motivo.trim();
        this.atualizadaEm = Instant.now();
    }

    public void devolverAoBot() {
        garantirAberta();
        this.modo = ModoAtendimento.BOT;
        this.atendenteId = null;
        this.motivoTransferencia = null;
        this.atualizadaEm = Instant.now();
    }

    public boolean botDeveResponder() {
        return status == StatusConversa.ABERTA && modo == ModoAtendimento.BOT;
    }

    public void encerrar() {
        garantirAberta();
        this.status = StatusConversa.ENCERRADA;
        this.atualizadaEm = Instant.now();
    }

    public void vincularCliente(UUID clienteId) {
        if (clienteId == null) {
            throw new DomainException("Cliente é obrigatório para vincular à conversa");
        }
        this.clienteId = clienteId;
        this.atualizadaEm = Instant.now();
    }

    private void garantirAberta() {
        if (status != StatusConversa.ABERTA) {
            throw new DomainException("Conversa encerrada não pode ser alterada");
        }
    }

    public UUID id() {
        return id;
    }

    public String telefoneWhatsapp() {
        return telefoneWhatsapp;
    }

    public UUID clienteId() {
        return clienteId;
    }

    public StatusConversa status() {
        return status;
    }

    public UUID atendenteId() {
        return atendenteId;
    }

    public ModoAtendimento modo() {
        return modo;
    }

    public String motivoTransferencia() {
        return motivoTransferencia;
    }

    public Instant criadaEm() {
        return criadaEm;
    }

    public Instant atualizadaEm() {
        return atualizadaEm;
    }
}

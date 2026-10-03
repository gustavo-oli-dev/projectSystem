package com.empresax.sistema.atendimento.mensagem;

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

@Entity
@Table(name = "mensagens")
public class Mensagem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID conversaId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrigemMensagem origem;

    @Column
    private String conteudo;

    @Column
    private String idExternoWhatsapp;

    @Column(nullable = false, updatable = false)
    private Instant enviadaEm;

    protected Mensagem() {
        // exigido pelo JPA
    }

    public Mensagem(UUID conversaId, OrigemMensagem origem, String conteudo, String idExternoWhatsapp) {
        this.conversaId = validarConversa(conversaId);
        this.origem = validarOrigem(origem);
        this.conteudo = conteudo;
        this.idExternoWhatsapp = idExternoWhatsapp;
        this.enviadaEm = Instant.now();
    }

    private static UUID validarConversa(UUID conversaId) {
        if (conversaId == null) {
            throw new DomainException("Mensagem precisa estar vinculada a uma conversa");
        }
        return conversaId;
    }

    private static OrigemMensagem validarOrigem(OrigemMensagem origem) {
        if (origem == null) {
            throw new DomainException("Origem da mensagem (cliente ou atendente) é obrigatória");
        }
        return origem;
    }

    public UUID id() {
        return id;
    }

    public UUID conversaId() {
        return conversaId;
    }

    public OrigemMensagem origem() {
        return origem;
    }

    public String conteudo() {
        return conteudo;
    }

    public String idExternoWhatsapp() {
        return idExternoWhatsapp;
    }

    public Instant enviadaEm() {
        return enviadaEm;
    }
}

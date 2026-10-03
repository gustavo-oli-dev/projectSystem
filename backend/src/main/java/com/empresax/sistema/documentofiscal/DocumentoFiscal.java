package com.empresax.sistema.documentofiscal;

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
 * Representa um documento fiscal (NF-e ou NFS-e) ligado a um Pedido. O XML autorizado e o
 * protocolo da SEFAZ/prefeitura SÃO o documento fiscal — o DANFE/DANFSE em PDF é derivado deles,
 * nunca armazenado como fonte da verdade (ver DECISOES.md).
 *
 * Modelo de domínio pronto para a fatia seguinte: a emissão de verdade (chamada à SEFAZ ou a um
 * provedor fiscal) ainda não está implementada — depende do regime tributário da empresa e das
 * credenciais do provedor escolhido (pendências A1 em DECISOES.md e itens em PENDENCIAS.md).
 */
@Entity
@Table(name = "documentos_fiscais")
public class DocumentoFiscal {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID pedidoId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoDocumentoFiscal tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusDocumentoFiscal status;

    @Column(columnDefinition = "text")
    private String xmlAutorizado;

    private String protocolo;

    private String motivoRejeicao;

    @Column(nullable = false, updatable = false)
    private Instant criadoEm;

    @Column(nullable = false)
    private Instant atualizadoEm;

    protected DocumentoFiscal() {
        // exigido pelo JPA
    }

    public DocumentoFiscal(UUID pedidoId, TipoDocumentoFiscal tipo) {
        this.pedidoId = validarPedido(pedidoId);
        this.tipo = validarTipo(tipo);
        this.status = StatusDocumentoFiscal.PENDENTE;
        this.criadoEm = Instant.now();
        this.atualizadoEm = this.criadoEm;
    }

    private static UUID validarPedido(UUID pedidoId) {
        if (pedidoId == null) {
            throw new DomainException("Documento fiscal precisa estar vinculado a um pedido");
        }
        return pedidoId;
    }

    private static TipoDocumentoFiscal validarTipo(TipoDocumentoFiscal tipo) {
        if (tipo == null) {
            throw new DomainException("Tipo do documento fiscal (NF-e ou NFS-e) é obrigatório");
        }
        return tipo;
    }

    public void autorizar(String xmlAutorizado, String protocolo) {
        garantirPendente();
        if (xmlAutorizado == null || xmlAutorizado.isBlank()) {
            throw new DomainException("XML autorizado é obrigatório para autorizar o documento fiscal");
        }
        if (protocolo == null || protocolo.isBlank()) {
            throw new DomainException("Protocolo é obrigatório para autorizar o documento fiscal");
        }
        this.xmlAutorizado = xmlAutorizado;
        this.protocolo = protocolo;
        this.status = StatusDocumentoFiscal.AUTORIZADO;
        this.atualizadoEm = Instant.now();
    }

    public void rejeitar(String motivo) {
        garantirPendente();
        if (motivo == null || motivo.isBlank()) {
            throw new DomainException("Motivo da rejeição é obrigatório");
        }
        this.motivoRejeicao = motivo;
        this.status = StatusDocumentoFiscal.REJEITADO;
        this.atualizadoEm = Instant.now();
    }

    public void cancelar() {
        if (status != StatusDocumentoFiscal.AUTORIZADO) {
            throw new DomainException("Só é possível cancelar um documento fiscal autorizado");
        }
        this.status = StatusDocumentoFiscal.CANCELADO;
        this.atualizadoEm = Instant.now();
    }

    private void garantirPendente() {
        if (status != StatusDocumentoFiscal.PENDENTE) {
            throw new DomainException("Documento fiscal já foi processado e não pode mudar de estado");
        }
    }

    public UUID id() {
        return id;
    }

    public UUID pedidoId() {
        return pedidoId;
    }

    public TipoDocumentoFiscal tipo() {
        return tipo;
    }

    public StatusDocumentoFiscal status() {
        return status;
    }

    public String xmlAutorizado() {
        return xmlAutorizado;
    }

    public String protocolo() {
        return protocolo;
    }

    public String motivoRejeicao() {
        return motivoRejeicao;
    }

    public Instant criadoEm() {
        return criadoEm;
    }

    public Instant atualizadoEm() {
        return atualizadoEm;
    }
}

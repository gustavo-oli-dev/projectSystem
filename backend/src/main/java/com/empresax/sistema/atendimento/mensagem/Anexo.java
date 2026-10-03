package com.empresax.sistema.atendimento.mensagem;

import com.empresax.sistema.common.domain.DomainException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

/**
 * Metadado do anexo (decisão D8 em DECISOES.md). Os bytes vivem no object storage
 * (ArmazenamentoObjetos); aqui só a referência (chaveObjeto) e o que descreve o arquivo.
 */
@Entity
@Table(name = "anexos")
public class Anexo {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID mensagemId;

    @Column(nullable = false)
    private String mimeType;

    @Column(nullable = false)
    private long tamanhoBytes;

    @Column(nullable = false, length = 64)
    private String checksumSha256;

    @Column(nullable = false)
    private String chaveObjeto;

    @Column
    private String nomeArquivoOriginal;

    protected Anexo() {
        // exigido pelo JPA
    }

    public Anexo(
            UUID mensagemId,
            String mimeType,
            long tamanhoBytes,
            String checksumSha256,
            String chaveObjeto,
            String nomeArquivoOriginal
    ) {
        this.mensagemId = validarMensagem(mensagemId);
        this.mimeType = validarMimeType(mimeType);
        this.tamanhoBytes = validarTamanho(tamanhoBytes);
        this.checksumSha256 = validarChecksum(checksumSha256);
        this.chaveObjeto = validarChaveObjeto(chaveObjeto);
        this.nomeArquivoOriginal = nomeArquivoOriginal;
    }

    private static UUID validarMensagem(UUID mensagemId) {
        if (mensagemId == null) {
            throw new DomainException("Anexo precisa estar vinculado a uma mensagem");
        }
        return mensagemId;
    }

    private static String validarMimeType(String mimeType) {
        if (mimeType == null || mimeType.isBlank()) {
            throw new DomainException("Tipo MIME do anexo é obrigatório");
        }
        return mimeType;
    }

    private static long validarTamanho(long tamanhoBytes) {
        if (tamanhoBytes <= 0) {
            throw new DomainException("Tamanho do anexo deve ser maior que zero");
        }
        return tamanhoBytes;
    }

    private static String validarChecksum(String checksum) {
        if (checksum == null || checksum.isBlank()) {
            throw new DomainException("Checksum do anexo é obrigatório");
        }
        return checksum;
    }

    private static String validarChaveObjeto(String chaveObjeto) {
        if (chaveObjeto == null || chaveObjeto.isBlank()) {
            throw new DomainException("Chave do objeto no armazenamento é obrigatória");
        }
        return chaveObjeto;
    }

    public UUID id() {
        return id;
    }

    public UUID mensagemId() {
        return mensagemId;
    }

    public String mimeType() {
        return mimeType;
    }

    public long tamanhoBytes() {
        return tamanhoBytes;
    }

    public String checksumSha256() {
        return checksumSha256;
    }

    public String chaveObjeto() {
        return chaveObjeto;
    }

    public String nomeArquivoOriginal() {
        return nomeArquivoOriginal;
    }
}

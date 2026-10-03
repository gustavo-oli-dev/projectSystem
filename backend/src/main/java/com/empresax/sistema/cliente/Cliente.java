package com.empresax.sistema.cliente;

import com.empresax.sistema.common.domain.DomainException;
import com.empresax.sistema.shared.documento.Documento;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "clientes", uniqueConstraints = @UniqueConstraint(columnNames = "documento"))
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String nome;

    @Convert(converter = DocumentoConverter.class)
    @Column(nullable = false, unique = true)
    private Documento documento;

    @Column(nullable = false)
    private String telefoneWhatsapp;

    @Column(nullable = false, updatable = false)
    private Instant criadoEm;

    protected Cliente() {
        // exigido pelo JPA
    }

    public Cliente(String nome, Documento documento, String telefoneWhatsapp) {
        this.nome = validarNome(nome);
        this.documento = validarDocumento(documento);
        this.telefoneWhatsapp = validarTelefone(telefoneWhatsapp);
        this.criadoEm = Instant.now();
    }

    private static String validarNome(String nome) {
        if (nome == null || nome.isBlank()) {
            throw new DomainException("Nome do cliente é obrigatório");
        }
        return nome.trim();
    }

    private static Documento validarDocumento(Documento documento) {
        if (documento == null) {
            throw new DomainException("Documento (CPF ou CNPJ) do cliente é obrigatório");
        }
        return documento;
    }

    private static String validarTelefone(String telefone) {
        if (telefone == null || telefone.isBlank()) {
            throw new DomainException("Telefone do WhatsApp do cliente é obrigatório");
        }
        return telefone.trim();
    }

    public void atualizarNome(String novoNome) {
        this.nome = validarNome(novoNome);
    }

    public void atualizarTelefoneWhatsapp(String novoTelefone) {
        this.telefoneWhatsapp = validarTelefone(novoTelefone);
    }

    public UUID id() {
        return id;
    }

    public String nome() {
        return nome;
    }

    public Documento documento() {
        return documento;
    }

    public String telefoneWhatsapp() {
        return telefoneWhatsapp;
    }

    public Instant criadoEm() {
        return criadoEm;
    }
}

package com.empresax.sistema.compras.contato;

import com.empresax.sistema.common.domain.DomainException;
import com.empresax.sistema.shared.documento.Documento;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Fornecedor, transportadora ou outro contato da empresa (D34). O CNPJ/CPF, quando informado, é
 * validado e único: é por ele que o fornecedor de uma nota é reconhecido.
 */
@Entity
@Table(name = "contatos")
public class Contato {

    private static final int TAMANHO_MAXIMO_NOME = 150;
    private static final int TAMANHO_MAXIMO_TELEFONE = 30;
    private static final int TAMANHO_MAXIMO_EMAIL = 150;
    private static final int TAMANHO_MAXIMO_OBSERVACAO = 500;
    private static final Pattern EMAIL_VALIDO = Pattern.compile("[^@\\s]+@[^@\\s]+\\.[^@\\s]+");

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoContato tipo;

    @Column(nullable = false, length = TAMANHO_MAXIMO_NOME)
    private String nome;

    @Column(length = 14, unique = true)
    private String documento;

    @Column(length = TAMANHO_MAXIMO_TELEFONE)
    private String telefone;

    @Column(length = TAMANHO_MAXIMO_EMAIL)
    private String email;

    @Column(length = TAMANHO_MAXIMO_OBSERVACAO)
    private String observacao;

    @Column(nullable = false)
    private boolean ativo;

    @Column(nullable = false, updatable = false)
    private Instant criadoEm;

    protected Contato() {
        // exigido pelo JPA
    }

    public Contato(TipoContato tipo, String nome, String documento, String telefone, String email, String observacao) {
        alterar(tipo, nome, documento, telefone, email, observacao);
        this.ativo = true;
        this.criadoEm = Instant.now();
    }

    public void alterar(TipoContato tipo, String nome, String documento, String telefone, String email, String observacao) {
        if (tipo == null) {
            throw new DomainException("Informe o tipo do contato");
        }
        this.tipo = tipo;
        this.nome = obrigatorio(nome, "Informe o nome do contato", TAMANHO_MAXIMO_NOME);
        this.documento = documentoValido(documento);
        this.telefone = opcional(telefone, TAMANHO_MAXIMO_TELEFONE);
        this.email = emailValido(email);
        this.observacao = opcional(observacao, TAMANHO_MAXIMO_OBSERVACAO);
    }

    public void desativar() {
        this.ativo = false;
    }

    public void ativar() {
        this.ativo = true;
    }

    /** CNPJ ou CPF validado (dígitos verificadores), guardado sem pontuação. */
    private static String documentoValido(String documento) {
        if (documento == null || documento.isBlank()) {
            return null;
        }
        return Documento.criar(documento.trim()).valor();
    }

    private static String emailValido(String email) {
        String limpo = opcional(email, TAMANHO_MAXIMO_EMAIL);
        if (limpo != null && !EMAIL_VALIDO.matcher(limpo).matches()) {
            throw new DomainException("E-mail do contato inválido");
        }
        return limpo;
    }

    private static String obrigatorio(String valor, String mensagem, int tamanhoMaximo) {
        String limpo = opcional(valor, tamanhoMaximo);
        if (limpo == null) {
            throw new DomainException(mensagem);
        }
        return limpo;
    }

    private static String opcional(String valor, int tamanhoMaximo) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        String limpo = valor.trim();
        if (limpo.length() > tamanhoMaximo) {
            throw new DomainException("Texto muito longo (máximo de " + tamanhoMaximo + " caracteres)");
        }
        return limpo;
    }

    public UUID id() {
        return id;
    }

    public TipoContato tipo() {
        return tipo;
    }

    public String nome() {
        return nome;
    }

    public Optional<String> documento() {
        return Optional.ofNullable(documento);
    }

    public Optional<String> telefone() {
        return Optional.ofNullable(telefone);
    }

    public Optional<String> email() {
        return Optional.ofNullable(email);
    }

    public Optional<String> observacao() {
        return Optional.ofNullable(observacao);
    }

    public boolean ativo() {
        return ativo;
    }
}

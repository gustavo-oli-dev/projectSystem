package com.empresax.sistema.acesso;

import com.empresax.sistema.common.domain.DomainException;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Cargo configurável pela tela: um nome e o conjunto de permissões que ele concede. */
@Entity
@Table(name = "cargos")
public class Cargo {

    private static final int TAMANHO_MAXIMO_NOME = 80;
    private static final int TAMANHO_MAXIMO_DESCRICAO = 255;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = TAMANHO_MAXIMO_NOME)
    private String nome;

    @Column(length = TAMANHO_MAXIMO_DESCRICAO)
    private String descricao;

    @ElementCollection
    @CollectionTable(name = "cargo_permissoes", joinColumns = @JoinColumn(name = "cargo_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "permissao", nullable = false)
    private Set<Permissao> permissoes = new HashSet<>();

    @Column(nullable = false, updatable = false)
    private Instant criadoEm;

    protected Cargo() {
        // exigido pelo JPA
    }

    public Cargo(String nome, String descricao, Set<Permissao> permissoes) {
        this.nome = validarNome(nome);
        this.descricao = validarDescricao(descricao);
        this.permissoes = new HashSet<>(validarPermissoes(permissoes));
        this.criadoEm = Instant.now();
    }

    private static String validarNome(String nome) {
        if (nome == null || nome.isBlank()) {
            throw new DomainException("Nome do perfil de acesso é obrigatório");
        }
        String nomeLimpo = nome.trim();
        if (nomeLimpo.length() > TAMANHO_MAXIMO_NOME) {
            throw new DomainException("Nome do perfil de acesso deve ter no máximo " + TAMANHO_MAXIMO_NOME + " caracteres");
        }
        return nomeLimpo;
    }

    private static String validarDescricao(String descricao) {
        if (descricao == null || descricao.isBlank()) {
            return null;
        }
        String descricaoLimpa = descricao.trim();
        if (descricaoLimpa.length() > TAMANHO_MAXIMO_DESCRICAO) {
            throw new DomainException("Descrição do perfil de acesso deve ter no máximo " + TAMANHO_MAXIMO_DESCRICAO + " caracteres");
        }
        return descricaoLimpa;
    }

    private static Set<Permissao> validarPermissoes(Set<Permissao> permissoes) {
        if (permissoes == null || permissoes.isEmpty()) {
            throw new DomainException("O perfil de acesso precisa de ao menos uma permissão");
        }
        return permissoes;
    }

    public void redefinir(String novoNome, String novaDescricao, Set<Permissao> novasPermissoes) {
        String nomeValidado = validarNome(novoNome);
        String descricaoValidada = validarDescricao(novaDescricao);
        Set<Permissao> permissoesValidadas = validarPermissoes(novasPermissoes);
        this.nome = nomeValidado;
        this.descricao = descricaoValidada;
        this.permissoes.clear();
        this.permissoes.addAll(permissoesValidadas);
    }

    public boolean possui(Permissao permissao) {
        return permissoes.contains(permissao);
    }

    public UUID id() {
        return id;
    }

    public String nome() {
        return nome;
    }

    public String descricao() {
        return descricao;
    }

    public Set<Permissao> permissoes() {
        return permissoes.isEmpty()
                ? Collections.emptySet()
                : Collections.unmodifiableSet(EnumSet.copyOf(permissoes));
    }

    public Instant criadoEm() {
        return criadoEm;
    }
}

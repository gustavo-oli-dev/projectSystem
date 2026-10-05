package com.empresax.sistema.compras.contas;

import com.empresax.sistema.common.domain.DomainException;
import com.empresax.sistema.shared.dinheiro.Dinheiro;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

/**
 * Uma conta da empresa a pagar (D34): parcela da nota de um fornecedor ou despesa lançada à mão
 * (luz, aluguel, frete). Aberta → paga (com quem e quando) ou cancelada.
 */
@Entity
@Table(name = "contas_a_pagar")
public class ContaAPagar {

    private static final int TAMANHO_MAXIMO_DESCRICAO = 200;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(updatable = false)
    private UUID contatoId;

    @Column(nullable = false, updatable = false, length = TAMANHO_MAXIMO_DESCRICAO)
    private String descricao;

    @Column(nullable = false, updatable = false)
    private Dinheiro valor;

    @Column(nullable = false, updatable = false)
    private LocalDate vencimento;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusContaAPagar status;

    @Column(updatable = false)
    private UUID notaEntradaId;

    @Column(nullable = false, updatable = false)
    private String criadaPor;

    @Column(nullable = false, updatable = false)
    private Instant criadaEm;

    @Column
    private Instant pagaEm;

    @Column
    private String pagaPor;

    protected ContaAPagar() {
        // exigido pelo JPA
    }

    private ContaAPagar(UUID contatoId, String descricao, Dinheiro valor, LocalDate vencimento, UUID notaEntradaId, String criadaPor) {
        if (descricao == null || descricao.isBlank()) {
            throw new DomainException("Informe a descrição da conta");
        }
        if (descricao.trim().length() > TAMANHO_MAXIMO_DESCRICAO) {
            throw new DomainException("Descrição muito longa (máximo de " + TAMANHO_MAXIMO_DESCRICAO + " caracteres)");
        }
        if (valor == null || valor.valor().signum() == 0) {
            throw new DomainException("O valor da conta precisa ser maior que zero");
        }
        if (vencimento == null) {
            throw new DomainException("Informe o vencimento da conta");
        }
        if (criadaPor == null || criadaPor.isBlank()) {
            throw new DomainException("Conta precisa de quem a lançou");
        }
        this.contatoId = contatoId;
        this.descricao = descricao.trim();
        this.valor = valor;
        this.vencimento = vencimento;
        this.notaEntradaId = notaEntradaId;
        this.criadaPor = criadaPor;
        this.criadaEm = Instant.now();
        this.status = StatusContaAPagar.ABERTA;
    }

    /** Despesa lançada à mão (o contato é opcional: "conta de luz" pode não ter cadastro). */
    public static ContaAPagar lancar(UUID contatoId, String descricao, Dinheiro valor, LocalDate vencimento, String criadaPor) {
        return new ContaAPagar(contatoId, descricao, valor, vencimento, null, criadaPor);
    }

    /** Parcela (duplicata) da nota de entrada de um fornecedor. */
    public static ContaAPagar parcelaDaNota(
            UUID fornecedorId, UUID notaEntradaId, String descricao, Dinheiro valor, LocalDate vencimento, String criadaPor
    ) {
        if (fornecedorId == null || notaEntradaId == null) {
            throw new DomainException("Parcela de nota precisa do fornecedor e da nota");
        }
        return new ContaAPagar(fornecedorId, descricao, valor, vencimento, notaEntradaId, criadaPor);
    }

    public void pagar(String quem) {
        garantirAberta();
        if (quem == null || quem.isBlank()) {
            throw new DomainException("Informe quem pagou a conta");
        }
        this.status = StatusContaAPagar.PAGA;
        this.pagaPor = quem;
        this.pagaEm = Instant.now();
    }

    public void cancelar() {
        garantirAberta();
        this.status = StatusContaAPagar.CANCELADA;
    }

    /** Aberta e com o vencimento já passado. */
    public boolean vencida(LocalDate hoje) {
        return status == StatusContaAPagar.ABERTA && vencimento.isBefore(hoje);
    }

    private void garantirAberta() {
        if (status != StatusContaAPagar.ABERTA) {
            throw new DomainException("Esta conta já foi paga ou cancelada");
        }
    }

    public UUID id() {
        return id;
    }

    public Optional<UUID> contatoId() {
        return Optional.ofNullable(contatoId);
    }

    public String descricao() {
        return descricao;
    }

    public Dinheiro valor() {
        return valor;
    }

    public LocalDate vencimento() {
        return vencimento;
    }

    public StatusContaAPagar status() {
        return status;
    }

    public Optional<UUID> notaEntradaId() {
        return Optional.ofNullable(notaEntradaId);
    }

    public Optional<Instant> pagaEm() {
        return Optional.ofNullable(pagaEm);
    }

    public Optional<String> pagaPor() {
        return Optional.ofNullable(pagaPor);
    }
}

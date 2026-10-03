package com.empresax.sistema.produto.estoque;

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
import java.util.Optional;
import java.util.UUID;

/**
 * Registro imutável de cada mudança no estoque: explica de onde veio o número atual (quem deu
 * entrada, qual venda tirou, qual reembolso devolveu). Nunca é alterado nem apagado.
 */
@Entity
@Table(name = "movimentacoes_estoque")
public class MovimentacaoEstoque {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, updatable = false)
    private UUID produtoId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private TipoMovimentacaoEstoque tipo;

    @Column(nullable = false, updatable = false)
    private int quantidade;

    @Column(nullable = false, updatable = false)
    private int saldoApos;

    @Column(updatable = false)
    private UUID pedidoId;

    @Column(nullable = false, updatable = false)
    private String responsavel;

    @Column(nullable = false, updatable = false)
    private Instant criadaEm;

    protected MovimentacaoEstoque() {
        // exigido pelo JPA
    }

    private MovimentacaoEstoque(
            UUID produtoId, TipoMovimentacaoEstoque tipo, int quantidade, int saldoApos, UUID pedidoId, String responsavel
    ) {
        if (produtoId == null || tipo == null) {
            throw new DomainException("Movimentação de estoque precisa de produto e tipo");
        }
        if (quantidade <= 0 || saldoApos < 0) {
            throw new DomainException("Movimentação de estoque com quantidade ou saldo inválido");
        }
        if (responsavel == null || responsavel.isBlank()) {
            throw new DomainException("Movimentação de estoque precisa de um responsável");
        }
        this.produtoId = produtoId;
        this.tipo = tipo;
        this.quantidade = quantidade;
        this.saldoApos = saldoApos;
        this.pedidoId = pedidoId;
        this.responsavel = responsavel;
        this.criadaEm = Instant.now();
    }

    public static MovimentacaoEstoque entrada(UUID produtoId, int quantidade, int saldoApos, String responsavel) {
        return new MovimentacaoEstoque(produtoId, TipoMovimentacaoEstoque.ENTRADA, quantidade, saldoApos, null, responsavel);
    }

    public static MovimentacaoEstoque venda(UUID produtoId, int quantidade, int saldoApos, UUID pedidoId, String responsavel) {
        return new MovimentacaoEstoque(produtoId, TipoMovimentacaoEstoque.VENDA, quantidade, saldoApos,
                exigirPedido(pedidoId), responsavel);
    }

    public static MovimentacaoEstoque devolucao(UUID produtoId, int quantidade, int saldoApos, UUID pedidoId, String responsavel) {
        return new MovimentacaoEstoque(produtoId, TipoMovimentacaoEstoque.DEVOLUCAO, quantidade, saldoApos,
                exigirPedido(pedidoId), responsavel);
    }

    private static UUID exigirPedido(UUID pedidoId) {
        if (pedidoId == null) {
            throw new DomainException("Venda e devolução de estoque precisam do pedido de origem");
        }
        return pedidoId;
    }

    public UUID id() {
        return id;
    }

    public UUID produtoId() {
        return produtoId;
    }

    public TipoMovimentacaoEstoque tipo() {
        return tipo;
    }

    public int quantidade() {
        return quantidade;
    }

    public int saldoApos() {
        return saldoApos;
    }

    public Optional<UUID> pedidoId() {
        return Optional.ofNullable(pedidoId);
    }

    public String responsavel() {
        return responsavel;
    }

    public Instant criadaEm() {
        return criadaEm;
    }
}

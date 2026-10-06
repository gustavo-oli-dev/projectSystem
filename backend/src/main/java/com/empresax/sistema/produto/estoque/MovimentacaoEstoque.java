package com.empresax.sistema.produto.estoque;

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
import java.util.Optional;
import java.util.UUID;

/**
 * Registro imutável de cada mudança no estoque: explica de onde veio o número atual (quem deu
 * entrada, qual venda tirou, qual reembolso devolveu). Nunca é alterado nem apagado.
 */
@Entity
@Table(name = "movimentacoes_estoque")
public class MovimentacaoEstoque {

    private static final int TAMANHO_MAXIMO_OBSERVACAO = 200;

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

    /** Entrada que veio de uma nota de compra (XML do fornecedor). */
    @Column(updatable = false)
    private UUID notaEntradaId;

    /** Só em perda. */
    @Enumerated(EnumType.STRING)
    @Column(updatable = false, length = 30)
    private MotivoPerda motivo;

    @Column(updatable = false, length = TAMANHO_MAXIMO_OBSERVACAO)
    private String observacao;

    /** Custo do produto na hora da perda (o relatório mostra quanto se perdeu em dinheiro). */
    @Column(updatable = false)
    private Dinheiro custoUnitario;

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

    /** Entrada pela nota do fornecedor: o histórico mostra de qual nota veio. */
    public static MovimentacaoEstoque entradaPorNota(UUID produtoId, int quantidade, int saldoApos, UUID notaEntradaId, String responsavel) {
        if (notaEntradaId == null) {
            throw new DomainException("Entrada por nota precisa da nota");
        }
        MovimentacaoEstoque entrada = new MovimentacaoEstoque(produtoId, TipoMovimentacaoEstoque.ENTRADA, quantidade, saldoApos, null, responsavel);
        entrada.notaEntradaId = notaEntradaId;
        return entrada;
    }

    /**
     * Produto que saiu sem ser vendido. "Outro" exige observação; o custo (se cadastrado) fica
     * congelado para o relatório de perdas.
     */
    public static MovimentacaoEstoque perda(
            UUID produtoId, int quantidade, int saldoApos, MotivoPerda motivo, String observacao, Dinheiro custoUnitario,
            String responsavel
    ) {
        if (motivo == null) {
            throw new DomainException("Informe o motivo da perda");
        }
        String observacaoLimpa = observacao == null || observacao.isBlank() ? null : observacao.trim();
        if (motivo.exigeObservacao() && observacaoLimpa == null) {
            throw new DomainException("Escreva o motivo da retirada");
        }
        if (observacaoLimpa != null && observacaoLimpa.length() > TAMANHO_MAXIMO_OBSERVACAO) {
            throw new DomainException("Observação muito longa (máximo de " + TAMANHO_MAXIMO_OBSERVACAO + " caracteres)");
        }
        MovimentacaoEstoque perda = new MovimentacaoEstoque(
                produtoId, TipoMovimentacaoEstoque.PERDA, quantidade, saldoApos, null, responsavel);
        perda.motivo = motivo;
        perda.observacao = observacaoLimpa;
        perda.custoUnitario = custoUnitario;
        return perda;
    }

    /** Acerto do inventário: diferença positiva = sobrou; negativa = faltou. */
    public static MovimentacaoEstoque ajusteDeInventario(UUID produtoId, int diferenca, int saldoApos, Dinheiro custoUnitario, String responsavel) {
        if (diferenca == 0) {
            throw new DomainException("Inventário sem diferença não gera movimentação");
        }
        TipoMovimentacaoEstoque tipo = diferenca > 0 ? TipoMovimentacaoEstoque.INVENTARIO_SOBRA : TipoMovimentacaoEstoque.INVENTARIO_FALTA;
        MovimentacaoEstoque ajuste = new MovimentacaoEstoque(produtoId, tipo, Math.abs(diferenca), saldoApos, null, responsavel);
        ajuste.custoUnitario = custoUnitario;
        return ajuste;
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

    public Optional<UUID> notaEntradaId() {
        return Optional.ofNullable(notaEntradaId);
    }

    public Optional<MotivoPerda> motivo() {
        return Optional.ofNullable(motivo);
    }

    public Optional<String> observacao() {
        return Optional.ofNullable(observacao);
    }

    public Optional<Dinheiro> custoUnitario() {
        return Optional.ofNullable(custoUnitario);
    }
}

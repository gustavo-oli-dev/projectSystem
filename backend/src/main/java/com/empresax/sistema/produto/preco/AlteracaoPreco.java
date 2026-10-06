package com.empresax.sistema.produto.preco;

import com.empresax.sistema.common.domain.DomainException;
import com.empresax.sistema.shared.dinheiro.Dinheiro;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/** Uma mudança de preço de um produto (D39): registro imutável de quem mudou, de quanto para quanto. */
@Entity
@Table(name = "alteracoes_preco")
public class AlteracaoPreco {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, updatable = false)
    private UUID produtoId;

    @Column(nullable = false, updatable = false)
    private Dinheiro precoAnterior;

    @Column(nullable = false, updatable = false)
    private Dinheiro precoNovo;

    @Column(nullable = false, updatable = false)
    private String alteradoPor;

    @Column(nullable = false, updatable = false)
    private Instant alteradoEm;

    protected AlteracaoPreco() {
        // exigido pelo JPA
    }

    public AlteracaoPreco(UUID produtoId, Dinheiro precoAnterior, Dinheiro precoNovo, String alteradoPor) {
        if (produtoId == null || precoAnterior == null || precoNovo == null) {
            throw new DomainException("Alteração de preço precisa do produto e dos dois preços");
        }
        if (alteradoPor == null || alteradoPor.isBlank()) {
            throw new DomainException("Alteração de preço precisa de quem alterou");
        }
        this.produtoId = produtoId;
        this.precoAnterior = precoAnterior;
        this.precoNovo = precoNovo;
        this.alteradoPor = alteradoPor;
        this.alteradoEm = Instant.now();
    }

    public UUID produtoId() {
        return produtoId;
    }

    public Dinheiro precoAnterior() {
        return precoAnterior;
    }

    public Dinheiro precoNovo() {
        return precoNovo;
    }

    public Instant alteradoEm() {
        return alteradoEm;
    }
}

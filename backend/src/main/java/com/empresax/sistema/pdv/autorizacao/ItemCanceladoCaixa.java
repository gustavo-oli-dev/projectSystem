package com.empresax.sistema.pdv.autorizacao;

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

/**
 * Item tirado da venda depois de lido no caixa, com a autorização de um gerente (D35). Imutável:
 * é a trilha que a conferência usa (cancelamento de item é ponto clássico de fraude).
 */
@Entity
@Table(name = "itens_cancelados_caixa")
public class ItemCanceladoCaixa {

    private static final int TAMANHO_MAXIMO_DESCRICAO = 200;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, updatable = false)
    private UUID sessaoCaixaId;

    @Column(nullable = false, updatable = false)
    private UUID produtoId;

    @Column(nullable = false, updatable = false, length = TAMANHO_MAXIMO_DESCRICAO)
    private String descricao;

    @Column(nullable = false, updatable = false)
    private int quantidade;

    @Column(nullable = false, updatable = false)
    private Dinheiro precoUnitario;

    @Column(nullable = false, updatable = false)
    private String operador;

    @Column(nullable = false, updatable = false)
    private String autorizadoPor;

    @Column(nullable = false, updatable = false)
    private Instant canceladoEm;

    protected ItemCanceladoCaixa() {
        // exigido pelo JPA
    }

    public ItemCanceladoCaixa(
            UUID sessaoCaixaId, UUID produtoId, String descricao, int quantidade, Dinheiro precoUnitario,
            String operador, String autorizadoPor
    ) {
        if (sessaoCaixaId == null || produtoId == null || precoUnitario == null) {
            throw new DomainException("Cancelamento de item precisa do caixa, do produto e do preço");
        }
        if (quantidade <= 0) {
            throw new DomainException("A quantidade cancelada precisa ser maior que zero");
        }
        if (operador == null || operador.isBlank() || autorizadoPor == null || autorizadoPor.isBlank()) {
            throw new DomainException("Cancelamento de item precisa do operador e de quem autorizou");
        }
        this.sessaoCaixaId = sessaoCaixaId;
        this.produtoId = produtoId;
        this.descricao = descricao.length() > TAMANHO_MAXIMO_DESCRICAO ? descricao.substring(0, TAMANHO_MAXIMO_DESCRICAO) : descricao;
        this.quantidade = quantidade;
        this.precoUnitario = precoUnitario;
        this.operador = operador;
        this.autorizadoPor = autorizadoPor;
        this.canceladoEm = Instant.now();
    }

    public Dinheiro valor() {
        return precoUnitario.multiplicar(quantidade);
    }

    public UUID id() {
        return id;
    }

    public String descricao() {
        return descricao;
    }

    public int quantidade() {
        return quantidade;
    }

    public String operador() {
        return operador;
    }

    public String autorizadoPor() {
        return autorizadoPor;
    }

    public Instant canceladoEm() {
        return canceladoEm;
    }
}

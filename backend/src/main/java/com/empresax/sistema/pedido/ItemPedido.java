package com.empresax.sistema.pedido;

import com.empresax.sistema.common.domain.DomainException;
import com.empresax.sistema.shared.dinheiro.Dinheiro;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import java.util.UUID;

/**
 * Linha de um Pedido. Guarda uma cópia (snapshot) da descrição e do preço no momento da venda —
 * um pedido já feito não pode mudar de valor se o Produto/Servico original mudar de preço depois.
 */
@Embeddable
public class ItemPedido {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoItem tipo;

    @Column(nullable = false)
    private UUID referenciaId;

    @Column(nullable = false)
    private String descricao;

    @Column(nullable = false)
    private Dinheiro precoUnitario;

    @Column(nullable = false)
    private int quantidade;

    protected ItemPedido() {
        // exigido pelo JPA
    }

    public ItemPedido(TipoItem tipo, UUID referenciaId, String descricao, Dinheiro precoUnitario, int quantidade) {
        this.tipo = validarTipo(tipo);
        this.referenciaId = validarReferencia(referenciaId);
        this.descricao = validarDescricao(descricao);
        this.precoUnitario = validarPreco(precoUnitario);
        this.quantidade = validarQuantidade(quantidade);
    }

    private static TipoItem validarTipo(TipoItem tipo) {
        if (tipo == null) {
            throw new DomainException("Tipo do item (produto ou serviço) é obrigatório");
        }
        return tipo;
    }

    private static UUID validarReferencia(UUID referenciaId) {
        if (referenciaId == null) {
            throw new DomainException("Item de pedido precisa referenciar um produto ou serviço");
        }
        return referenciaId;
    }

    private static String validarDescricao(String descricao) {
        if (descricao == null || descricao.isBlank()) {
            throw new DomainException("Descrição do item de pedido é obrigatória");
        }
        return descricao;
    }

    private static Dinheiro validarPreco(Dinheiro precoUnitario) {
        if (precoUnitario == null) {
            throw new DomainException("Preço unitário do item de pedido é obrigatório");
        }
        return precoUnitario;
    }

    private static int validarQuantidade(int quantidade) {
        if (quantidade <= 0) {
            throw new DomainException("Quantidade do item de pedido deve ser maior que zero");
        }
        return quantidade;
    }

    public Dinheiro subtotal() {
        return precoUnitario.multiplicar(quantidade);
    }

    public TipoItem tipo() {
        return tipo;
    }

    public UUID referenciaId() {
        return referenciaId;
    }

    public String descricao() {
        return descricao;
    }

    public Dinheiro precoUnitario() {
        return precoUnitario;
    }

    public int quantidade() {
        return quantidade;
    }
}

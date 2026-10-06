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

    /**
     * Desconto total do item: promoção + a parte do desconto do gerente (rateio). Zero = sem desconto.
     * Os relatórios e a NFC-e usam este total.
     */
    @Column(nullable = false)
    private Dinheiro desconto = Dinheiro.zero();

    /** Quanto do desconto veio da promoção do produto (D38). */
    @Column(nullable = false)
    private Dinheiro descontoPromocao = Dinheiro.zero();

    /** Custo unitário no momento da venda (snapshot), para o lucro não mudar se o custo mudar depois. */
    @Column
    private Dinheiro custoUnitario;

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

    /** custoUnitario nulo = custo não informado (o lucro desse item fica de fora). */
    public ItemPedido(
            TipoItem tipo, UUID referenciaId, String descricao, Dinheiro precoUnitario, int quantidade, Dinheiro custoUnitario
    ) {
        this(tipo, referenciaId, descricao, precoUnitario, quantidade);
        this.custoUnitario = custoUnitario;
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

    /** Valor cheio do item (preço × quantidade), antes do desconto. */
    public Dinheiro valorBruto() {
        return precoUnitario.multiplicar(quantidade);
    }

    /** Valor do item já com o desconto. */
    public Dinheiro subtotal() {
        return valorBruto().subtrair(desconto);
    }

    /** Valor do item com a promoção, antes do desconto do gerente. */
    public Dinheiro valorComPromocao() {
        return valorBruto().subtrair(descontoPromocao);
    }

    /** Desconto da promoção do produto (D38). Vem antes de qualquer desconto do gerente. */
    public void receberPromocao(Dinheiro descontoDaPromocao) {
        if (descontoDaPromocao == null || !descontoDaPromocao.menorQue(valorBruto())) {
            throw new DomainException("Promoção maior que o valor do item \"" + descricao + "\"");
        }
        if (!desconto.equals(descontoPromocao)) {
            throw new DomainException("A promoção entra antes do desconto do gerente");
        }
        this.descontoPromocao = descontoDaPromocao;
        this.desconto = descontoDaPromocao;
    }

    /** Recebe a sua parte do desconto do gerente (nunca maior que o valor com a promoção). */
    void receberDesconto(Dinheiro parte) {
        if (parte == null || valorComPromocao().menorQue(parte)) {
            throw new DomainException("Desconto maior que o valor do item \"" + descricao + "\"");
        }
        this.desconto = descontoPromocao.somar(parte);
    }

    /** Desconto total (promoção + gerente). */
    public Dinheiro desconto() {
        return desconto;
    }

    public Dinheiro descontoPromocao() {
        return descontoPromocao;
    }

    /** Só a parte do desconto autorizada pelo gerente. */
    public Dinheiro descontoDoGerente() {
        return desconto.subtrair(descontoPromocao);
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

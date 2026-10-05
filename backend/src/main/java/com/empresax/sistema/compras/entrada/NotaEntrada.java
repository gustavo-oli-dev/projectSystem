package com.empresax.sistema.compras.entrada;

import com.empresax.sistema.common.domain.DomainException;
import com.empresax.sistema.shared.dinheiro.Dinheiro;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Nota de compra que já entrou no estoque (D34). A chave de acesso é única: a mesma nota nunca
 * entra duas vezes. Guarda só os itens ligados a um produto do sistema (os ignorados não entram).
 */
@Entity
@Table(name = "notas_entrada")
public class NotaEntrada {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, updatable = false, length = 44, unique = true)
    private String chaveAcesso;

    @Column(nullable = false, updatable = false, length = 9)
    private String numero;

    @Column(nullable = false, updatable = false, length = 3)
    private String serie;

    @Column(nullable = false, updatable = false)
    private Instant emitidaEm;

    @Column(nullable = false, updatable = false)
    private UUID fornecedorId;

    @Column(nullable = false, updatable = false)
    private Dinheiro valorTotal;

    @ElementCollection
    @CollectionTable(name = "itens_nota_entrada", joinColumns = @JoinColumn(name = "nota_entrada_id"))
    @OrderBy("ordem ASC")
    private List<Item> itens = new ArrayList<>();

    @Column(nullable = false, updatable = false)
    private String registradaPor;

    @Column(nullable = false, updatable = false)
    private Instant registradaEm;

    protected NotaEntrada() {
        // exigido pelo JPA
    }

    public NotaEntrada(NotaDoFornecedor nota, UUID fornecedorId, List<Item> itensLigados, String registradaPor) {
        if (fornecedorId == null) {
            throw new DomainException("Nota de entrada precisa do fornecedor");
        }
        if (itensLigados == null || itensLigados.isEmpty()) {
            throw new DomainException("Ligue ao menos um item da nota a um produto do sistema");
        }
        if (registradaPor == null || registradaPor.isBlank()) {
            throw new DomainException("Nota de entrada precisa de quem a lançou");
        }
        this.chaveAcesso = nota.chaveAcesso();
        this.numero = nota.numero();
        this.serie = nota.serie();
        this.emitidaEm = nota.emitidaEm();
        this.fornecedorId = fornecedorId;
        this.valorTotal = new Dinheiro(nota.valorTotal());
        this.itens = new ArrayList<>(itensLigados);
        this.registradaPor = registradaPor;
        this.registradaEm = Instant.now();
    }

    /** Item da nota ligado a um produto do sistema. */
    @Embeddable
    public static class Item {

        @Column(name = "ordem", nullable = false)
        private int ordem;

        @Column(name = "produto_id", nullable = false)
        private UUID produtoId;

        @Column(name = "descricao_na_nota", nullable = false, length = 200)
        private String descricaoNaNota;

        @Column(name = "quantidade", nullable = false)
        private int quantidade;

        /** Custo por unidade como veio na nota (até 4 casas, como a NF-e permite). */
        @Column(name = "custo_unitario", nullable = false)
        private BigDecimal custoUnitario;

        protected Item() {
            // exigido pelo JPA
        }

        public Item(int ordem, UUID produtoId, String descricaoNaNota, int quantidade, BigDecimal custoUnitario) {
            if (produtoId == null || quantidade <= 0 || custoUnitario == null || custoUnitario.signum() < 0) {
                throw new DomainException("Item da nota com produto, quantidade ou custo inválido");
            }
            this.ordem = ordem;
            this.produtoId = produtoId;
            this.descricaoNaNota = descricaoNaNota.length() > 200 ? descricaoNaNota.substring(0, 200) : descricaoNaNota;
            this.quantidade = quantidade;
            this.custoUnitario = custoUnitario;
        }

        public UUID produtoId() {
            return produtoId;
        }

        public int quantidade() {
            return quantidade;
        }

        public BigDecimal custoUnitario() {
            return custoUnitario;
        }
    }

    public UUID id() {
        return id;
    }

    public String chaveAcesso() {
        return chaveAcesso;
    }

    public String numero() {
        return numero;
    }

    public UUID fornecedorId() {
        return fornecedorId;
    }

    public Dinheiro valorTotal() {
        return valorTotal;
    }

    public List<Item> itens() {
        return Collections.unmodifiableList(itens);
    }

    public String registradaPor() {
        return registradaPor;
    }

    public Instant registradaEm() {
        return registradaEm;
    }
}

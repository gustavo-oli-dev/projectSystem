package com.empresax.sistema.produto.embalagem;

import com.empresax.sistema.common.domain.DomainException;
import com.empresax.sistema.produto.Gtin;
import com.empresax.sistema.shared.dinheiro.Dinheiro;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Uma forma de vender o produto em quantidade (D41): "Fardo com 12", "Caixa com 6". O estoque fica
 * sempre em unidades — vender 1 fardo baixa 12. Tem preço próprio e, se tiver, código de barras
 * próprio. Para mudar, remove e cria outra (as vendas antigas guardam o que valia).
 */
@Entity
@Table(name = "embalagens_produto")
public class Embalagem {

    private static final int TAMANHO_MAXIMO_NOME = 60;
    private static final int MINIMO_UNIDADES = 2;
    private static final int MAXIMO_UNIDADES = 1000;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, updatable = false)
    private UUID produtoId;

    @Column(nullable = false, updatable = false, length = TAMANHO_MAXIMO_NOME)
    private String nome;

    @Column(updatable = false)
    private String codigoBarras;

    @Column(nullable = false, updatable = false)
    private int unidades;

    @Column(nullable = false, updatable = false)
    private Dinheiro preco;

    @Column(nullable = false)
    private boolean ativa = true;

    @Column(nullable = false, updatable = false)
    private Instant criadaEm;

    protected Embalagem() {
        // exigido pelo JPA
    }

    public Embalagem(UUID produtoId, String nome, String codigoBarras, int unidades, Dinheiro preco) {
        if (produtoId == null) {
            throw new DomainException("Embalagem precisa do produto");
        }
        if (nome == null || nome.isBlank()) {
            throw new DomainException("Dê um nome à embalagem (ex.: Fardo com 12)");
        }
        if (nome.trim().length() > TAMANHO_MAXIMO_NOME) {
            throw new DomainException("Nome da embalagem muito longo (máximo de " + TAMANHO_MAXIMO_NOME + " caracteres)");
        }
        if (unidades < MINIMO_UNIDADES || unidades > MAXIMO_UNIDADES) {
            throw new DomainException("A embalagem precisa ter entre " + MINIMO_UNIDADES + " e " + MAXIMO_UNIDADES + " unidades");
        }
        if (preco == null || preco.valor().signum() == 0) {
            throw new DomainException("Informe o preço da embalagem");
        }
        this.produtoId = produtoId;
        this.nome = nome.trim();
        this.codigoBarras = Gtin.validarOpcional(codigoBarras);
        this.unidades = unidades;
        this.preco = preco;
        this.criadaEm = Instant.now();
    }

    public void desativar() {
        this.ativa = false;
    }

    /** Custo da embalagem a partir do custo da unidade (o lucro do fardo bate com o das unidades). */
    public Dinheiro custoA(Dinheiro custoDaUnidade) {
        return custoDaUnidade.multiplicar(unidades);
    }

    public boolean doProduto(UUID outroProdutoId) {
        return produtoId.equals(outroProdutoId);
    }

    public UUID id() {
        return id;
    }

    public UUID produtoId() {
        return produtoId;
    }

    public String nome() {
        return nome;
    }

    public Optional<String> codigoBarras() {
        return Optional.ofNullable(codigoBarras);
    }

    public int unidades() {
        return unidades;
    }

    public Dinheiro preco() {
        return preco;
    }

    public boolean ativa() {
        return ativa;
    }
}

package com.empresax.sistema.promocao;

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
 * Promoção de um produto num período (D38): preço de oferta ou "leve X pague Y". Vale do primeiro ao
 * último dia, inclusive, até alguém encerrar antes. Não muda depois de criada — para mudar, encerre
 * e crie outra (o histórico do que valeu em cada venda fica intacto).
 */
@Entity
@Table(name = "promocoes")
public class Promocao {

    /** Grupo grande demais não é promoção de supermercado, é erro de digitação. */
    private static final int MAXIMO_LEVE = 100;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, updatable = false)
    private UUID produtoId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private TipoPromocao tipo;

    @Column(updatable = false)
    private Dinheiro precoOferta;

    @Column(updatable = false)
    private Integer leve;

    @Column(updatable = false)
    private Integer pague;

    @Column(nullable = false, updatable = false)
    private LocalDate inicio;

    @Column(nullable = false, updatable = false)
    private LocalDate fim;

    @Column
    private Instant encerradaEm;

    @Column
    private String encerradaPor;

    @Column(nullable = false, updatable = false)
    private String criadaPor;

    @Column(nullable = false, updatable = false)
    private Instant criadaEm;

    protected Promocao() {
        // exigido pelo JPA
    }

    private Promocao(UUID produtoId, TipoPromocao tipo, LocalDate inicio, LocalDate fim, String criadaPor) {
        if (produtoId == null) {
            throw new DomainException("Escolha o produto da promoção");
        }
        if (inicio == null || fim == null) {
            throw new DomainException("Informe o primeiro e o último dia da promoção");
        }
        if (fim.isBefore(inicio)) {
            throw new DomainException("O último dia da promoção não pode ser antes do primeiro");
        }
        if (criadaPor == null || criadaPor.isBlank()) {
            throw new DomainException("Promoção precisa de quem a criou");
        }
        this.produtoId = produtoId;
        this.tipo = tipo;
        this.inicio = inicio;
        this.fim = fim;
        this.criadaPor = criadaPor;
        this.criadaEm = Instant.now();
    }

    /** "De R$ 9,90 por R$ 7,90": a oferta precisa ser menor que o preço de hoje. */
    public static Promocao precoDeOferta(
            UUID produtoId, Dinheiro precoOferta, Dinheiro precoAtual, LocalDate inicio, LocalDate fim, String criadaPor
    ) {
        if (precoOferta == null || precoOferta.valor().signum() == 0) {
            throw new DomainException("Informe o preço de oferta");
        }
        if (!precoOferta.menorQue(precoAtual)) {
            throw new DomainException("O preço de oferta precisa ser menor que o preço atual (" + precoAtual.valor() + ")");
        }
        Promocao promocao = new Promocao(produtoId, TipoPromocao.PRECO_OFERTA, inicio, fim, criadaPor);
        promocao.precoOferta = precoOferta;
        return promocao;
    }

    /** "Leve 3, pague 2": leve maior que pague, e paga ao menos uma unidade. */
    public static Promocao levePague(UUID produtoId, int leve, int pague, LocalDate inicio, LocalDate fim, String criadaPor) {
        if (pague < 1 || leve <= pague) {
            throw new DomainException("No \"leve X pague Y\", leve precisa ser maior que pague, e pague ao menos 1");
        }
        if (leve > MAXIMO_LEVE) {
            throw new DomainException("Leve no máximo " + MAXIMO_LEVE + " unidades");
        }
        Promocao promocao = new Promocao(produtoId, TipoPromocao.LEVE_PAGUE, inicio, fim, criadaPor);
        promocao.leve = leve;
        promocao.pague = pague;
        return promocao;
    }

    public boolean valeNoDia(LocalDate dia) {
        return encerradaEm == null && !dia.isBefore(inicio) && !dia.isAfter(fim);
    }

    /** Duas promoções do mesmo produto não podem valer no mesmo dia (não se somam). */
    public boolean coincideCom(LocalDate outroInicio, LocalDate outroFim) {
        return encerradaEm == null && !outroFim.isBefore(inicio) && !outroInicio.isAfter(fim);
    }

    /** Desconto para esta quantidade ao preço normal. Zero quando não dá direito (ex.: leve 3 e levou 2). */
    public Dinheiro descontoPara(Dinheiro precoNormal, int quantidade) {
        return tipo.descontoPara(this, precoNormal, quantidade);
    }

    public void encerrar(String quem, LocalDate hoje) {
        if (encerradaEm != null) {
            throw new DomainException("Esta promoção já foi encerrada");
        }
        if (fim.isBefore(hoje)) {
            throw new DomainException("Esta promoção já terminou");
        }
        this.encerradaEm = Instant.now();
        this.encerradaPor = quem;
    }

    public SituacaoPromocao situacao(LocalDate hoje) {
        if (encerradaEm != null) {
            return SituacaoPromocao.ENCERRADA;
        }
        if (hoje.isBefore(inicio)) {
            return SituacaoPromocao.AGENDADA;
        }
        return hoje.isAfter(fim) ? SituacaoPromocao.TERMINADA : SituacaoPromocao.VALENDO;
    }

    public UUID id() {
        return id;
    }

    public UUID produtoId() {
        return produtoId;
    }

    public TipoPromocao tipo() {
        return tipo;
    }

    public Optional<Dinheiro> precoOferta() {
        return Optional.ofNullable(precoOferta);
    }

    public Optional<Integer> leve() {
        return Optional.ofNullable(leve);
    }

    public Optional<Integer> pague() {
        return Optional.ofNullable(pague);
    }

    public LocalDate inicio() {
        return inicio;
    }

    public LocalDate fim() {
        return fim;
    }

    public String criadaPor() {
        return criadaPor;
    }

    public Optional<String> encerradaPor() {
        return Optional.ofNullable(encerradaPor);
    }
}

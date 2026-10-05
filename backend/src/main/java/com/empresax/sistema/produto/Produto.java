package com.empresax.sistema.produto;

import com.empresax.sistema.common.domain.DomainException;
import com.empresax.sistema.shared.dinheiro.Dinheiro;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Produto vendido pela empresa — gera NF-e quando vendido (RF03).
 */
@Entity
@Table(name = "produtos")
public class Produto {

    private static final Pattern NCM_VALIDO = Pattern.compile("\\d{8}");
    private static final Pattern GTIN_VALIDO = Pattern.compile("\\d{8}|\\d{12,14}");
    private static final int PESO_GTIN_IMPAR = 3;
    private static final int MODULO_GTIN = 10;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String nome;

    @Column
    private String descricao;

    @Column(nullable = false, length = 8)
    private String ncm;

    @Column(nullable = false)
    private String unidadeMedida;

    @Column(nullable = false)
    private Dinheiro precoUnitario;

    @Column(nullable = false)
    private boolean ativo;

    /**
     * Código de barras EAN/GTIN (8, 12, 13 ou 14 dígitos). Opcional; é o campo cEAN da NF-e e a
     * chave que o futuro app de escaneamento usará para achar o produto.
     */
    @Column(unique = true, length = 14)
    private String codigoBarras;

    /** Quanto o produto custa para a empresa (compra/produção). Opcional; sem ele o lucro fica incompleto. */
    @Column
    private Dinheiro custoUnitario;

    /** Nunca negativo (o banco também garante com CHECK). Só muda pelas operações de estoque abaixo. */
    @Column(nullable = false)
    private int quantidadeEmEstoque;

    protected Produto() {
        // exigido pelo JPA
    }

    public Produto(String nome, String descricao, String ncm, String unidadeMedida, Dinheiro precoUnitario) {
        this(nome, descricao, ncm, unidadeMedida, precoUnitario, null);
    }

    public Produto(
            String nome, String descricao, String ncm, String unidadeMedida, Dinheiro precoUnitario, String codigoBarras
    ) {
        this.nome = validarNome(nome);
        this.descricao = descricao;
        this.ncm = validarNcm(ncm);
        this.unidadeMedida = validarUnidadeMedida(unidadeMedida);
        this.precoUnitario = validarPreco(precoUnitario);
        this.codigoBarras = validarCodigoBarras(codigoBarras);
        this.ativo = true;
    }

    private static String validarNome(String nome) {
        if (nome == null || nome.isBlank()) {
            throw new DomainException("Nome do produto é obrigatório");
        }
        return nome.trim();
    }

    private static String validarNcm(String ncm) {
        if (ncm == null || !NCM_VALIDO.matcher(ncm).matches()) {
            throw new DomainException("NCM deve conter exatamente 8 dígitos numéricos");
        }
        return ncm;
    }

    private static String validarUnidadeMedida(String unidadeMedida) {
        if (unidadeMedida == null || unidadeMedida.isBlank()) {
            throw new DomainException("Unidade de medida do produto é obrigatória");
        }
        return unidadeMedida.trim().toUpperCase();
    }

    private static Dinheiro validarPreco(Dinheiro precoUnitario) {
        if (precoUnitario == null) {
            throw new DomainException("Preço unitário do produto é obrigatório");
        }
        return precoUnitario;
    }

    public void atualizar(String nome, String descricao, Dinheiro precoUnitario, String codigoBarras) {
        String nomeValidado = validarNome(nome);
        Dinheiro precoValidado = validarPreco(precoUnitario);
        String codigoValidado = validarCodigoBarras(codigoBarras);
        this.nome = nomeValidado;
        this.descricao = descricao;
        this.precoUnitario = precoValidado;
        this.codigoBarras = codigoValidado;
    }

    /** Aceita vazio (produto sem código). Confere o dígito verificador GS1 — pega erro de digitação/leitura. */
    private static String validarCodigoBarras(String codigoBarras) {
        if (codigoBarras == null || codigoBarras.isBlank()) {
            return null;
        }
        String codigo = codigoBarras.trim();
        if (!GTIN_VALIDO.matcher(codigo).matches() || !digitoVerificadorGtinConfere(codigo)) {
            throw new DomainException("Código de barras inválido (EAN/GTIN de 8, 12, 13 ou 14 dígitos)");
        }
        return codigo;
    }

    private static boolean digitoVerificadorGtinConfere(String codigo) {
        int soma = 0;
        int ultimo = codigo.length() - 1;
        for (int posicao = 0; posicao < ultimo; posicao++) {
            int digito = codigo.charAt(ultimo - 1 - posicao) - '0';
            soma += posicao % 2 == 0 ? digito * PESO_GTIN_IMPAR : digito;
        }
        int verificadorEsperado = (MODULO_GTIN - soma % MODULO_GTIN) % MODULO_GTIN;
        return verificadorEsperado == codigo.charAt(ultimo) - '0';
    }

    /** Nulo = custo não informado. Mudar o custo não altera o lucro de vendas já feitas (cada item guarda o seu). */
    public void definirCusto(Dinheiro custo) {
        this.custoUnitario = custo;
    }

    public void darEntradaNoEstoque(int quantidade) {
        this.quantidadeEmEstoque += validarQuantidade(quantidade);
    }

    /** Baixa por venda confirmada. Recusa vender o que não existe — estoque nunca fica negativo. */
    public void baixarDoEstoque(int quantidade) {
        int quantidadeValidada = validarQuantidade(quantidade);
        if (!possuiEmEstoque(quantidadeValidada)) {
            throw new DomainException("Estoque insuficiente para \"" + nome + "\": disponível "
                    + quantidadeEmEstoque + ", pedido " + quantidadeValidada);
        }
        this.quantidadeEmEstoque -= quantidadeValidada;
    }

    /**
     * Inventário: o estoque passa a ser o que foi contado na prateleira. Devolve a diferença
     * (positiva = sobrou, negativa = faltou) para registrar o acerto.
     */
    public int ajustarAoContado(int quantidadeContada) {
        if (quantidadeContada < 0) {
            throw new DomainException("A quantidade contada não pode ser negativa");
        }
        int diferenca = quantidadeContada - quantidadeEmEstoque;
        this.quantidadeEmEstoque = quantidadeContada;
        return diferenca;
    }

    /** Volta ao estoque o que tinha saído por uma venda cancelada ou reembolsada. */
    public void devolverAoEstoque(int quantidade) {
        this.quantidadeEmEstoque += validarQuantidade(quantidade);
    }

    public boolean possuiEmEstoque(int quantidade) {
        return quantidade <= quantidadeEmEstoque;
    }

    private static int validarQuantidade(int quantidade) {
        if (quantidade <= 0) {
            throw new DomainException("Quantidade deve ser maior que zero");
        }
        return quantidade;
    }

    public void desativar() {
        this.ativo = false;
    }

    public void ativar() {
        this.ativo = true;
    }

    public UUID id() {
        return id;
    }

    public String nome() {
        return nome;
    }

    public String descricao() {
        return descricao;
    }

    public String ncm() {
        return ncm;
    }

    public String unidadeMedida() {
        return unidadeMedida;
    }

    public Dinheiro precoUnitario() {
        return precoUnitario;
    }

    public boolean ativo() {
        return ativo;
    }

    public int quantidadeEmEstoque() {
        return quantidadeEmEstoque;
    }

    public Optional<Dinheiro> custoUnitario() {
        return Optional.ofNullable(custoUnitario);
    }

    public Optional<String> codigoBarras() {
        return Optional.ofNullable(codigoBarras);
    }
}

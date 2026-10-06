package com.empresax.sistema.produto.fiscal;

import com.empresax.sistema.common.domain.DomainException;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Como o produto é tributado na nota (D42). Imutável: para mudar, troca o objeto inteiro.
 * <ul>
 *   <li>CST do ICMS (2 dígitos, regime normal) ou CSOSN (3 dígitos, Simples Nacional) — o regime da
 *   empresa ainda não foi decidido, então os dois são aceitos.</li>
 *   <li>Substituição tributária: o ICMS já foi pago antes (na indústria/distribuidor); exige o CEST
 *   e um CST/CSOSN de ST.</li>
 *   <li>Cesta básica: alíquota zero de IBS/CBS (reforma tributária, LC 214/2025, Anexo I).</li>
 *   <li>Classificação tributária (cClassTrib, 6 dígitos): o código do IBS/CBS no documento.</li>
 * </ul>
 */
@Embeddable
public class TributacaoProduto {

    private static final Pattern CST_OU_CSOSN = Pattern.compile("\\d{2}|\\d{3}");
    private static final Pattern CEST = Pattern.compile("\\d{7}");
    private static final Pattern CLASSIFICACAO = Pattern.compile("\\d{6}");
    private static final int ORIGEM_MAXIMA = 8;
    private static final BigDecimal CEM = BigDecimal.valueOf(100);
    /** CST com ICMS cobrado por substituição (regime normal). */
    private static final Set<String> CST_DE_ST = Set.of("10", "30", "60", "70", "90");
    /** CSOSN com substituição tributária (Simples Nacional). */
    private static final Set<String> CSOSN_DE_ST = Set.of("201", "202", "203", "500", "900");

    @Column(name = "origem_mercadoria")
    private Integer origem;

    @Column(name = "cst_icms", length = 3)
    private String cstIcms;

    @Column(name = "aliquota_icms")
    private BigDecimal aliquotaIcms;

    @Column(name = "substituicao_tributaria")
    private Boolean substituicaoTributaria;

    @Column(name = "cest", length = 7)
    private String cest;

    @Column(name = "cesta_basica")
    private Boolean cestaBasica;

    @Column(name = "classificacao_tributaria", length = 6)
    private String classificacaoTributaria;

    protected TributacaoProduto() {
        // exigido pelo JPA
    }

    public TributacaoProduto(
            int origem, String cstIcms, BigDecimal aliquotaIcms, boolean substituicaoTributaria, String cest,
            boolean cestaBasica, String classificacaoTributaria
    ) {
        if (origem < 0 || origem > ORIGEM_MAXIMA) {
            throw new DomainException("Origem da mercadoria vai de 0 (nacional) a 8");
        }
        String cst = limpar(cstIcms);
        if (cst == null || !CST_OU_CSOSN.matcher(cst).matches()) {
            throw new DomainException("Informe o CST do ICMS (2 dígitos) ou o CSOSN do Simples Nacional (3 dígitos)");
        }
        if (aliquotaIcms != null && (aliquotaIcms.signum() < 0 || aliquotaIcms.compareTo(CEM) > 0)) {
            throw new DomainException("A alíquota de ICMS precisa ficar entre 0% e 100%");
        }
        String cestLimpo = limpar(cest);
        if (cestLimpo != null && !CEST.matcher(cestLimpo).matches()) {
            throw new DomainException("O CEST tem 7 dígitos");
        }
        if (substituicaoTributaria) {
            exigirCoerenciaDaSubstituicao(cst, cestLimpo);
        }
        String classificacao = limpar(classificacaoTributaria);
        if (classificacao != null && !CLASSIFICACAO.matcher(classificacao).matches()) {
            throw new DomainException("A classificação tributária do IBS/CBS (cClassTrib) tem 6 dígitos");
        }
        this.origem = origem;
        this.cstIcms = cst;
        this.aliquotaIcms = aliquotaIcms;
        this.substituicaoTributaria = substituicaoTributaria;
        this.cest = cestLimpo;
        this.cestaBasica = cestaBasica;
        this.classificacaoTributaria = classificacao;
    }

    private static void exigirCoerenciaDaSubstituicao(String cst, String cest) {
        if (cest == null) {
            throw new DomainException("Produto com substituição tributária precisa do CEST");
        }
        boolean cstDeSt = cst.length() == 2 ? CST_DE_ST.contains(cst) : CSOSN_DE_ST.contains(cst);
        if (!cstDeSt) {
            throw new DomainException("Com substituição tributária, use um CST de ST (10, 30, 60, 70, 90) "
                    + "ou um CSOSN de ST (201, 202, 203, 500, 900)");
        }
    }

    private static String limpar(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }

    /** CSOSN tem 3 dígitos: empresa do Simples Nacional. */
    public boolean doSimplesNacional() {
        return cstIcms.length() == 3;
    }

    public int origem() {
        return origem;
    }

    public String cstIcms() {
        return cstIcms;
    }

    public Optional<BigDecimal> aliquotaIcms() {
        return Optional.ofNullable(aliquotaIcms);
    }

    public boolean substituicaoTributaria() {
        return Boolean.TRUE.equals(substituicaoTributaria);
    }

    public Optional<String> cest() {
        return Optional.ofNullable(cest);
    }

    public boolean cestaBasica() {
        return Boolean.TRUE.equals(cestaBasica);
    }

    public Optional<String> classificacaoTributaria() {
        return Optional.ofNullable(classificacaoTributaria);
    }
}

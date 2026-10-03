package com.empresax.sistema.servico;

import com.empresax.sistema.common.domain.DomainException;
import com.empresax.sistema.shared.dinheiro.Dinheiro;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Serviço prestado pela empresa — gera NFS-e quando vendido (RF03).
 */
@Entity
@Table(name = "servicos")
public class Servico {

    private static final Pattern CODIGO_LC116_VALIDO = Pattern.compile("\\d{2}\\.\\d{2}");
    private static final BigDecimal ALIQUOTA_ISS_MINIMA = new BigDecimal("2.00");
    private static final BigDecimal ALIQUOTA_ISS_MAXIMA = new BigDecimal("5.00");

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String nome;

    @Column
    private String descricao;

    @Column(nullable = false, length = 5)
    private String codigoServicoLc116;

    @Column(nullable = false)
    private BigDecimal aliquotaIss;

    @Column(nullable = false)
    private Dinheiro precoUnitario;

    @Column(nullable = false)
    private boolean ativo;

    protected Servico() {
        // exigido pelo JPA
    }

    public Servico(
            String nome,
            String descricao,
            String codigoServicoLc116,
            BigDecimal aliquotaIss,
            Dinheiro precoUnitario
    ) {
        this.nome = validarNome(nome);
        this.descricao = descricao;
        this.codigoServicoLc116 = validarCodigoLc116(codigoServicoLc116);
        this.aliquotaIss = validarAliquotaIss(aliquotaIss);
        this.precoUnitario = validarPreco(precoUnitario);
        this.ativo = true;
    }

    private static String validarNome(String nome) {
        if (nome == null || nome.isBlank()) {
            throw new DomainException("Nome do serviço é obrigatório");
        }
        return nome.trim();
    }

    private static String validarCodigoLc116(String codigo) {
        if (codigo == null || !CODIGO_LC116_VALIDO.matcher(codigo).matches()) {
            throw new DomainException("Código de serviço (LC 116/2003) deve estar no formato NN.NN");
        }
        return codigo;
    }

    private static BigDecimal validarAliquotaIss(BigDecimal aliquota) {
        if (aliquota == null) {
            throw new DomainException("Alíquota de ISS é obrigatória");
        }
        if (aliquota.compareTo(ALIQUOTA_ISS_MINIMA) < 0 || aliquota.compareTo(ALIQUOTA_ISS_MAXIMA) > 0) {
            throw new DomainException("Alíquota de ISS deve estar entre 2% e 5% (LC 116/2003, LC 157/2016)");
        }
        return aliquota;
    }

    private static Dinheiro validarPreco(Dinheiro precoUnitario) {
        if (precoUnitario == null) {
            throw new DomainException("Preço unitário do serviço é obrigatório");
        }
        return precoUnitario;
    }

    public void atualizar(String nome, String descricao, Dinheiro precoUnitario) {
        this.nome = validarNome(nome);
        this.descricao = descricao;
        this.precoUnitario = validarPreco(precoUnitario);
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

    public String codigoServicoLc116() {
        return codigoServicoLc116;
    }

    public BigDecimal aliquotaIss() {
        return aliquotaIss;
    }

    public Dinheiro precoUnitario() {
        return precoUnitario;
    }

    public boolean ativo() {
        return ativo;
    }
}

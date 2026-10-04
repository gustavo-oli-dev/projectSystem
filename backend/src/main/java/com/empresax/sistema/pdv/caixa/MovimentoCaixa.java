package com.empresax.sistema.pdv.caixa;

import com.empresax.sistema.common.domain.DomainException;
import com.empresax.sistema.shared.dinheiro.Dinheiro;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.MapKeyEnumerated;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Suprimento ou sangria dentro de um caixa aberto. Imutável depois de registrado (é trilha de
 * auditoria do dinheiro). O suprimento guarda as cédulas que entraram; a sangria, só o valor.
 */
@Entity
@Table(name = "movimentos_caixa")
public class MovimentoCaixa {

    private static final int TAMANHO_MAXIMO_MOTIVO = 200;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private TipoMovimentoCaixa tipo;

    @Column(nullable = false, updatable = false)
    private Dinheiro valor;

    @Column(nullable = false, updatable = false, length = TAMANHO_MAXIMO_MOTIVO)
    private String motivo;

    @ElementCollection
    @CollectionTable(name = "movimentos_caixa_cedulas", joinColumns = @JoinColumn(name = "movimento_caixa_id"))
    @MapKeyEnumerated(EnumType.STRING)
    @MapKeyColumn(name = "cedula")
    @Column(name = "quantidade")
    private Map<Cedula, Integer> cedulas = new HashMap<>();

    @Column(nullable = false, updatable = false)
    private String registradoPor;

    @Column(nullable = false, updatable = false)
    private Instant registradoEm;

    protected MovimentoCaixa() {
        // exigido pelo JPA
    }

    private MovimentoCaixa(TipoMovimentoCaixa tipo, Dinheiro valor, ContagemCedulas cedulas, String motivo, String registradoPor) {
        if (valor == null || valor.valor().signum() == 0) {
            throw new DomainException("O valor do movimento precisa ser maior que zero");
        }
        if (motivo == null || motivo.isBlank()) {
            throw new DomainException("Informe o motivo do movimento de caixa");
        }
        if (motivo.trim().length() > TAMANHO_MAXIMO_MOTIVO) {
            throw new DomainException("Motivo muito longo (máximo de " + TAMANHO_MAXIMO_MOTIVO + " caracteres)");
        }
        if (registradoPor == null || registradoPor.isBlank()) {
            throw new DomainException("Movimento de caixa precisa de quem o registrou");
        }
        this.tipo = tipo;
        this.valor = valor;
        this.cedulas = new HashMap<>(cedulas.quantidades());
        this.motivo = motivo.trim();
        this.registradoPor = registradoPor;
        this.registradoEm = Instant.now();
    }

    static MovimentoCaixa suprimento(ContagemCedulas cedulas, String motivo, String registradoPor) {
        return new MovimentoCaixa(TipoMovimentoCaixa.SUPRIMENTO, cedulas.total(), cedulas, motivo, registradoPor);
    }

    static MovimentoCaixa sangria(Dinheiro valor, String motivo, String registradoPor) {
        return new MovimentoCaixa(TipoMovimentoCaixa.SANGRIA, valor, ContagemCedulas.vazia(), motivo, registradoPor);
    }

    /** Positivo para o que entra na gaveta, negativo para o que sai. */
    BigDecimal efeitoNaGaveta() {
        return tipo == TipoMovimentoCaixa.SUPRIMENTO ? valor.valor() : valor.valor().negate();
    }


    public UUID id() {
        return id;
    }

    public TipoMovimentoCaixa tipo() {
        return tipo;
    }

    public Dinheiro valor() {
        return valor;
    }

    public String motivo() {
        return motivo;
    }

    public ContagemCedulas cedulas() {
        return new ContagemCedulas(cedulas);
    }

    public String registradoPor() {
        return registradoPor;
    }

    public Instant registradoEm() {
        return registradoEm;
    }
}

package com.empresax.sistema.pdv.caixa;

import com.empresax.sistema.common.domain.DomainException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/** Caixa físico da loja (Caixa 01, Caixa 02...). Cada abertura liga um caixa a um operador (D29). */
@Entity
@Table(name = "pontos_caixa")
public class PontoCaixa {

    private static final int NUMERO_MINIMO = 1;
    private static final int NUMERO_MAXIMO = 999;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, updatable = false)
    private int numero;

    @Column(nullable = false)
    private boolean ativo;

    @Column(nullable = false, updatable = false)
    private Instant criadoEm;

    protected PontoCaixa() {
        // exigido pelo JPA
    }

    public PontoCaixa(int numero) {
        if (numero < NUMERO_MINIMO || numero > NUMERO_MAXIMO) {
            throw new DomainException("O número do caixa deve ficar entre " + NUMERO_MINIMO + " e " + NUMERO_MAXIMO);
        }
        this.numero = numero;
        this.ativo = true;
        this.criadoEm = Instant.now();
    }

    /** "Caixa 01". */
    public String nome() {
        return nomeDoNumero(numero);
    }

    public static String nomeDoNumero(int numero) {
        return "Caixa %02d".formatted(numero);
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

    public int numero() {
        return numero;
    }

    public boolean ativo() {
        return ativo;
    }
}

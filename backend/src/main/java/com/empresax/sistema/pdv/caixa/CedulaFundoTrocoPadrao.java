package com.empresax.sistema.pdv.caixa;

import com.empresax.sistema.common.domain.DomainException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Uma linha do fundo de troco padrão definido pelo gerente: "cada caixa abre com 10 notas de R$ 5". */
@Entity
@Table(name = "fundo_troco_padrao")
public class CedulaFundoTrocoPadrao {

    @Id
    @Enumerated(EnumType.STRING)
    private Cedula cedula;

    @Column(nullable = false)
    private int quantidade;

    protected CedulaFundoTrocoPadrao() {
        // exigido pelo JPA
    }

    public CedulaFundoTrocoPadrao(Cedula cedula, int quantidade) {
        if (cedula == null || quantidade <= 0) {
            throw new DomainException("Fundo de troco padrão precisa de cédula e quantidade maior que zero");
        }
        this.cedula = cedula;
        this.quantidade = quantidade;
    }

    public Cedula cedula() {
        return cedula;
    }

    public int quantidade() {
        return quantidade;
    }
}

package com.empresax.sistema.pdv.caixa;

import com.empresax.sistema.common.domain.DomainException;
import com.empresax.sistema.pdv.FormaPagamentoPresencial;
import com.empresax.sistema.shared.dinheiro.Dinheiro;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import java.math.BigDecimal;

/**
 * Conferência de uma forma de pagamento da maquininha no fechamento: quanto o sistema registrou ×
 * quanto o relatório da maquininha mostra. Diferença = informado − sistema.
 */
@Embeddable
public class ConferenciaForma {

    @Enumerated(EnumType.STRING)
    @Column(name = "forma", nullable = false)
    private FormaPagamentoPresencial forma;

    @Column(name = "valor_sistema", nullable = false)
    private Dinheiro valorSistema;

    @Column(name = "valor_informado", nullable = false)
    private Dinheiro valorInformado;

    protected ConferenciaForma() {
        // exigido pelo JPA
    }

    ConferenciaForma(FormaPagamentoPresencial forma, Dinheiro valorSistema, Dinheiro valorInformado) {
        if (forma == null || !forma.passaPelaMaquininha()) {
            throw new DomainException("Só cartão e Pix da maquininha são conferidos por valor informado");
        }
        if (valorSistema == null || valorInformado == null) {
            throw new DomainException("Informe o valor de cada forma de pagamento no relatório da maquininha");
        }
        this.forma = forma;
        this.valorSistema = valorSistema;
        this.valorInformado = valorInformado;
    }

    public BigDecimal diferenca() {
        return valorInformado.valor().subtract(valorSistema.valor());
    }

    public FormaPagamentoPresencial forma() {
        return forma;
    }

    public Dinheiro valorSistema() {
        return valorSistema;
    }

    public Dinheiro valorInformado() {
        return valorInformado;
    }
}

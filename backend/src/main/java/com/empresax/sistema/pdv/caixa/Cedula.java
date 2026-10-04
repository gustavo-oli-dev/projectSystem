package com.empresax.sistema.pdv.caixa;

import com.empresax.sistema.shared.dinheiro.Dinheiro;

import java.math.BigDecimal;

/** Cédulas e moedas do real em circulação, da menor para a maior (é a ordem da tela de contagem). */
public enum Cedula {
    MOEDA_5_CENTAVOS("0.05"),
    MOEDA_10_CENTAVOS("0.10"),
    MOEDA_25_CENTAVOS("0.25"),
    MOEDA_50_CENTAVOS("0.50"),
    MOEDA_1_REAL("1.00"),
    NOTA_2("2.00"),
    NOTA_5("5.00"),
    NOTA_10("10.00"),
    NOTA_20("20.00"),
    NOTA_50("50.00"),
    NOTA_100("100.00"),
    NOTA_200("200.00");

    private final Dinheiro valor;

    Cedula(String valor) {
        this.valor = new Dinheiro(new BigDecimal(valor));
    }

    public Dinheiro valor() {
        return valor;
    }
}

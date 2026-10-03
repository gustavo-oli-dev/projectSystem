package com.empresax.sistema.shared.dinheiro;

import com.empresax.sistema.common.domain.DomainException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DinheiroTest {

    @Test
    void arredondaParaDuasCasasDecimais() {
        Dinheiro dinheiro = new Dinheiro(new BigDecimal("10.005"));

        assertThat(dinheiro.valor()).isEqualByComparingTo("10.01");
    }

    @Test
    void rejeitaValorNegativo() {
        assertThatThrownBy(() -> new Dinheiro(new BigDecimal("-0.01")))
                .isInstanceOf(DomainException.class);
    }

    @Test
    void somaDoisValores() {
        Dinheiro total = new Dinheiro(new BigDecimal("10.00")).somar(new Dinheiro(new BigDecimal("5.50")));

        assertThat(total.valor()).isEqualByComparingTo("15.50");
    }

    @Test
    void multiplicaPelaQuantidade() {
        Dinheiro subtotal = new Dinheiro(new BigDecimal("10.00")).multiplicar(3);

        assertThat(subtotal.valor()).isEqualByComparingTo("30.00");
    }
}

package com.empresax.sistema.pdv.caixa;

import com.empresax.sistema.common.domain.DomainException;
import com.empresax.sistema.shared.dinheiro.Dinheiro;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ContagemCedulasTest {

    @Test
    void totalSomaCedulasEMoedasSemErroDeArredondamento() {
        ContagemCedulas contagem = new ContagemCedulas(Map.of(
                Cedula.NOTA_50, 1, Cedula.MOEDA_25_CENTAVOS, 3, Cedula.MOEDA_5_CENTAVOS, 7));

        assertThat(contagem.total()).isEqualTo(new Dinheiro(new BigDecimal("51.10")));
    }

    @Test
    void quantidadeZeroEhDescartada() {
        ContagemCedulas contagem = new ContagemCedulas(Map.of(Cedula.NOTA_2, 0, Cedula.NOTA_10, 2));

        assertThat(contagem.quantidades()).containsOnlyKeys(Cedula.NOTA_10);
    }

    @Test
    void quantidadeNegativaEhRecusada() {
        assertThatThrownBy(() -> new ContagemCedulas(Map.of(Cedula.NOTA_2, -1)))
                .isInstanceOf(DomainException.class);
    }

    @Test
    void contagemVaziaTotalizaZero() {
        assertThat(ContagemCedulas.vazia().total()).isEqualTo(Dinheiro.zero());
        assertThat(ContagemCedulas.vazia().estaVazia()).isTrue();
    }
}

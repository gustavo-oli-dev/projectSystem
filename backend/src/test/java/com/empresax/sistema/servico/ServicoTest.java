package com.empresax.sistema.servico;

import com.empresax.sistema.common.domain.DomainException;
import com.empresax.sistema.shared.dinheiro.Dinheiro;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ServicoTest {

    private static final Dinheiro PRECO = new Dinheiro(new BigDecimal("150.00"));

    @Test
    void cadastraServicoAtivoPorPadrao() {
        Servico servico = new Servico("Consultoria", null, "01.01", new BigDecimal("3.00"), PRECO);

        assertThat(servico.ativo()).isTrue();
    }

    @Test
    void rejeitaCodigoLc116ForaDoFormato() {
        assertThatThrownBy(() -> new Servico("Consultoria", null, "0101", new BigDecimal("3.00"), PRECO))
                .isInstanceOf(DomainException.class);
    }

    @Test
    void rejeitaAliquotaIssAbaixoDoMinimoLegal() {
        assertThatThrownBy(() -> new Servico("Consultoria", null, "01.01", new BigDecimal("1.99"), PRECO))
                .isInstanceOf(DomainException.class);
    }

    @Test
    void rejeitaAliquotaIssAcimaDoMaximoLegal() {
        assertThatThrownBy(() -> new Servico("Consultoria", null, "01.01", new BigDecimal("5.01"), PRECO))
                .isInstanceOf(DomainException.class);
    }
}

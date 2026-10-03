package com.empresax.sistema.shared.documento;

import com.empresax.sistema.common.domain.DomainException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CpfTest {

    @Test
    void aceitaCpfValidoFormatado() {
        Cpf cpf = new Cpf("111.444.777-35");

        assertThat(cpf.valor()).isEqualTo("11144477735");
    }

    @ParameterizedTest
    @ValueSource(strings = {"00000000000", "11111111111", "12345678900", "111.444.777-36"})
    void rejeitaCpfComDigitoVerificadorInvalido(String cpfInvalido) {
        assertThatThrownBy(() -> new Cpf(cpfInvalido)).isInstanceOf(DomainException.class);
    }

    @Test
    void rejeitaCpfComQuantidadeErradaDeDigitos() {
        assertThatThrownBy(() -> new Cpf("123")).isInstanceOf(DomainException.class);
    }

    @Test
    void rejeitaCpfNulo() {
        assertThatThrownBy(() -> new Cpf(null)).isInstanceOf(DomainException.class);
    }
}

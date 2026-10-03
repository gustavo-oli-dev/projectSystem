package com.empresax.sistema.shared.documento;

import com.empresax.sistema.common.domain.DomainException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CnpjTest {

    @Test
    void aceitaCnpjNumericoValidoFormatado() {
        Cnpj cnpj = new Cnpj("11.222.333/0001-81");

        assertThat(cnpj.valor()).isEqualTo("11222333000181");
    }

    @ParameterizedTest
    @ValueSource(strings = {"11222333000180", "00000000000000", "123"})
    void rejeitaCnpjComDigitoVerificadorOuFormatoInvalido(String cnpjInvalido) {
        assertThatThrownBy(() -> new Cnpj(cnpjInvalido)).isInstanceOf(DomainException.class);
    }

    @Test
    void rejeitaCnpjNulo() {
        assertThatThrownBy(() -> new Cnpj(null)).isInstanceOf(DomainException.class);
    }
}

package com.empresax.sistema.cliente;

import com.empresax.sistema.common.domain.DomainException;
import com.empresax.sistema.shared.documento.Documento;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ClienteTest {

    @Test
    void cadastraClienteComDocumentoValido() {
        Documento cpf = Documento.criar("111.444.777-35");

        Cliente cliente = new Cliente("João Souza", cpf, "+5585999999999");

        assertThat(cliente.documento()).isEqualTo(cpf);
    }

    @Test
    void rejeitaClienteSemTelefone() {
        Documento cpf = Documento.criar("111.444.777-35");

        assertThatThrownBy(() -> new Cliente("João Souza", cpf, " "))
                .isInstanceOf(DomainException.class);
    }

    @Test
    void rejeitaClienteSemDocumento() {
        assertThatThrownBy(() -> new Cliente("João Souza", null, "+5585999999999"))
                .isInstanceOf(DomainException.class);
    }
}

package com.empresax.sistema.usuario.whatsapp;

import com.empresax.sistema.common.domain.DomainException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TelefoneWhatsAppTest {

    @Test
    void numeroDigitadoComMascaraViraFormatoDaWApiComDdi55() {
        assertThat(TelefoneWhatsApp.de("(85) 98888-7777").numero()).isEqualTo("5585988887777");
    }

    @Test
    void numeroQueJaTemDdiEhMantido() {
        assertThat(TelefoneWhatsApp.de("+55 85 98888-7777").numero()).isEqualTo("5585988887777");
    }

    @Test
    void fixoComOitoDigitosTambemEhAceito() {
        assertThat(TelefoneWhatsApp.de("85 3222-1111").numero()).isEqualTo("558532221111");
    }

    @Test
    void rejeitaNumeroCurtoDemais() {
        assertThatThrownBy(() -> TelefoneWhatsApp.de("98888-7777")).isInstanceOf(DomainException.class);
    }

    @Test
    void rejeitaTextoSemNumeros() {
        assertThatThrownBy(() -> TelefoneWhatsApp.de("meu zap")).isInstanceOf(DomainException.class);
    }
}

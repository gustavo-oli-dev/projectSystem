package com.empresax.sistema.usuario.whatsapp;

import com.empresax.sistema.common.domain.DomainException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VerificacaoWhatsAppTest {

    private static final Instant INICIO = Instant.parse("2026-10-03T12:00:00Z");
    private static final TelefoneWhatsApp TELEFONE = TelefoneWhatsApp.de("85988887777");

    private static VerificacaoWhatsApp novaVerificacao() {
        return new VerificacaoWhatsApp(UUID.randomUUID(), TELEFONE, "123456", INICIO);
    }

    @Test
    void codigoCorretoDentroDoPrazoDevolveOTelefoneVerificado() {
        TelefoneWhatsApp verificado = novaVerificacao().confirmar("123456", INICIO.plusSeconds(30));

        assertThat(verificado).isEqualTo(TELEFONE);
    }

    @Test
    void codigoErradoConsomeUmaTentativa() {
        VerificacaoWhatsApp verificacao = novaVerificacao();

        assertThatThrownBy(() -> verificacao.confirmar("000000", INICIO)).isInstanceOf(DomainException.class);
        assertThat(verificacao.tentativasRestantes()).isEqualTo(VerificacaoWhatsApp.TENTATIVAS_PERMITIDAS - 1);
    }

    @Test
    void depoisDeEsgotarAsTentativasNemOCodigoCertoFunciona() {
        VerificacaoWhatsApp verificacao = novaVerificacao();
        for (int tentativa = 0; tentativa < VerificacaoWhatsApp.TENTATIVAS_PERMITIDAS; tentativa++) {
            assertThatThrownBy(() -> verificacao.confirmar("000000", INICIO)).isInstanceOf(DomainException.class);
        }

        assertThatThrownBy(() -> verificacao.confirmar("123456", INICIO))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("esgotadas");
    }

    @Test
    void codigoVencidoEhRecusado() {
        Instant depoisDaValidade = INICIO.plus(VerificacaoWhatsApp.VALIDADE).plusSeconds(1);

        assertThatThrownBy(() -> novaVerificacao().confirmar("123456", depoisDaValidade))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("expirou");
    }

    @Test
    void novoEnvioSoDepoisDoIntervaloMinimo() {
        VerificacaoWhatsApp verificacao = novaVerificacao();

        assertThat(verificacao.permiteNovoEnvio(INICIO.plusSeconds(10))).isFalse();
        assertThat(verificacao.permiteNovoEnvio(INICIO.plus(VerificacaoWhatsApp.INTERVALO_MINIMO_ENTRE_ENVIOS))).isTrue();
    }
}

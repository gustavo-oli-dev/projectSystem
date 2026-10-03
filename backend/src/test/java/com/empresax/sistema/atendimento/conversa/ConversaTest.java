package com.empresax.sistema.atendimento.conversa;

import com.empresax.sistema.common.domain.DomainException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConversaTest {

    @Test
    void nasceAbertaSemClienteVinculado() {
        Conversa conversa = new Conversa("5585999999999", null);

        assertThat(conversa.status()).isEqualTo(StatusConversa.ABERTA);
        assertThat(conversa.clienteId()).isNull();
    }

    @Test
    void atribuirAtendenteDefineOAtendente() {
        Conversa conversa = new Conversa("5585999999999", null);
        UUID atendenteId = UUID.randomUUID();

        conversa.atribuirAtendente(atendenteId);

        assertThat(conversa.atendenteId()).isEqualTo(atendenteId);
    }

    @Test
    void naoPermiteAlterarConversaEncerrada() {
        Conversa conversa = new Conversa("5585999999999", null);
        conversa.encerrar();

        assertThatThrownBy(() -> conversa.atribuirAtendente(UUID.randomUUID()))
                .isInstanceOf(DomainException.class);
    }

    @Test
    void novaConversaComecaAtendidaPeloBot() {
        Conversa conversa = new Conversa("5585999999999", null);

        assertThat(conversa.modo()).isEqualTo(ModoAtendimento.BOT);
        assertThat(conversa.botDeveResponder()).isTrue();
    }

    @Test
    void atendenteAssumindoFazOBotParar() {
        Conversa conversa = new Conversa("5585999999999", null);

        conversa.atribuirAtendente(UUID.randomUUID());

        assertThat(conversa.modo()).isEqualTo(ModoAtendimento.HUMANO);
        assertThat(conversa.botDeveResponder()).isFalse();
    }

    @Test
    void transferenciaVaiParaFilaHumanaSemAtendenteEGuardaOMotivo() {
        Conversa conversa = new Conversa("5585999999999", null);

        conversa.transferirParaAtendente("Cliente pediu para falar com uma pessoa");

        assertThat(conversa.modo()).isEqualTo(ModoAtendimento.HUMANO);
        assertThat(conversa.atendenteId()).isNull();
        assertThat(conversa.motivoTransferencia()).isEqualTo("Cliente pediu para falar com uma pessoa");
    }

    @Test
    void devolverAoBotLimpaAtendenteEMotivo() {
        Conversa conversa = new Conversa("5585999999999", null);
        conversa.transferirParaAtendente("Bot indisponível");

        conversa.devolverAoBot();

        assertThat(conversa.botDeveResponder()).isTrue();
        assertThat(conversa.motivoTransferencia()).isNull();
    }

    @Test
    void conversaEncerradaNaoTemRespostaDoBot() {
        Conversa conversa = new Conversa("5585999999999", null);

        conversa.encerrar();

        assertThat(conversa.botDeveResponder()).isFalse();
    }

    @Test
    void rejeitaTelefoneEmBranco() {
        assertThatThrownBy(() -> new Conversa(" ", null)).isInstanceOf(DomainException.class);
    }
}

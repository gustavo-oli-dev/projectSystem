package com.empresax.sistema.atendimento.bot;

import com.empresax.sistema.atendimento.mensagem.Mensagem;
import com.empresax.sistema.atendimento.mensagem.OrigemMensagem;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class HistoricoParaIaTest {

    private static final UUID CONVERSA = UUID.randomUUID();

    @Test
    void juntaMensagensSeguidasDoClienteNumTurnoSo() {
        List<HistoricoParaIa.Turno> turnos = HistoricoParaIa.montar(List.of(
                mensagem(OrigemMensagem.CLIENTE, "Oi"),
                mensagem(OrigemMensagem.CLIENTE, "Tudo bem?")), 10);

        assertThat(turnos).containsExactly(new HistoricoParaIa.Turno("user", "Oi\nTudo bem?"));
    }

    @Test
    void botEAtendenteContamComoAEmpresaFalando() {
        List<HistoricoParaIa.Turno> turnos = HistoricoParaIa.montar(List.of(
                mensagem(OrigemMensagem.CLIENTE, "Oi"),
                mensagem(OrigemMensagem.BOT, "Olá!"),
                mensagem(OrigemMensagem.ATENDENTE, "Sou a Ana"),
                mensagem(OrigemMensagem.CLIENTE, "Meu pedido?")), 10);

        assertThat(turnos).extracting(HistoricoParaIa.Turno::papel)
                .containsExactly("user", "assistant", "user");
    }

    @Test
    void descartaTurnosDaEmpresaNoInicioPorqueAIaExigeComecarPeloCliente() {
        List<HistoricoParaIa.Turno> turnos = HistoricoParaIa.montar(List.of(
                mensagem(OrigemMensagem.CLIENTE, "mensagem antiga"),
                mensagem(OrigemMensagem.BOT, "resposta antiga"),
                mensagem(OrigemMensagem.CLIENTE, "nova")), 2);

        assertThat(turnos).containsExactly(new HistoricoParaIa.Turno("user", "nova"));
    }

    @Test
    void ignoraMensagensSemTexto() {
        List<HistoricoParaIa.Turno> turnos = HistoricoParaIa.montar(List.of(
                mensagem(OrigemMensagem.CLIENTE, null),
                mensagem(OrigemMensagem.CLIENTE, "texto")), 10);

        assertThat(turnos).containsExactly(new HistoricoParaIa.Turno("user", "texto"));
    }

    private static Mensagem mensagem(OrigemMensagem origem, String conteudo) {
        return new Mensagem(CONVERSA, origem, conteudo, null);
    }
}

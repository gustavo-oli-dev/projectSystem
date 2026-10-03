package com.empresax.sistema.atendimento.bot;

import com.empresax.sistema.atendimento.mensagem.Mensagem;
import com.empresax.sistema.atendimento.mensagem.OrigemMensagem;

import java.util.ArrayList;
import java.util.List;

/**
 * Converte as últimas mensagens da conversa em turnos para a Claude, que exige papéis alternados
 * (user/assistant) começando por "user". Mensagens seguidas do mesmo lado viram um turno só;
 * respostas do bot e do atendente contam como "assistant" (ambas são a empresa falando).
 */
public final class HistoricoParaIa {

    public static final String PAPEL_CLIENTE = "user";
    public static final String PAPEL_EMPRESA = "assistant";

    public record Turno(String papel, String texto) {
    }

    private HistoricoParaIa() {
    }

    public static List<Turno> montar(List<Mensagem> mensagensEmOrdem, int limite) {
        int inicio = Math.max(0, mensagensEmOrdem.size() - limite);
        List<Turno> turnos = new ArrayList<>();

        for (Mensagem mensagem : mensagensEmOrdem.subList(inicio, mensagensEmOrdem.size())) {
            if (mensagem.conteudo() == null || mensagem.conteudo().isBlank()) {
                continue;
            }
            String papel = mensagem.origem() == OrigemMensagem.CLIENTE ? PAPEL_CLIENTE : PAPEL_EMPRESA;
            adicionarOuJuntar(turnos, papel, mensagem.conteudo());
        }

        while (!turnos.isEmpty() && PAPEL_EMPRESA.equals(turnos.get(0).papel())) {
            turnos.remove(0);
        }
        return List.copyOf(turnos);
    }

    private static void adicionarOuJuntar(List<Turno> turnos, String papel, String texto) {
        if (!turnos.isEmpty() && turnos.get(turnos.size() - 1).papel().equals(papel)) {
            Turno anterior = turnos.remove(turnos.size() - 1);
            turnos.add(new Turno(papel, anterior.texto() + "\n" + texto));
            return;
        }
        turnos.add(new Turno(papel, texto));
    }
}

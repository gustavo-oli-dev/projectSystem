package com.empresax.sistema.atendimento.bot;

import java.util.ArrayList;
import java.util.List;

/**
 * Fotos de produto que a IA pediu para mandar durante uma resposta do bot (D43). Mutável de
 * propósito, como o PedidoDeTransferencia: preenchido pela ferramenta no loop, enviado no fim.
 */
final class FotosParaEnviar {

    /** Limite por resposta: o bot não vira um álbum (e não estoura o tamanho das mensagens). */
    static final int MAXIMO_POR_RESPOSTA = 3;

    record Foto(byte[] conteudo, String tipoMime, String legenda) {
    }

    private final List<Foto> fotos = new ArrayList<>();

    boolean cabeMais() {
        return fotos.size() < MAXIMO_POR_RESPOSTA;
    }

    void adicionar(byte[] conteudo, String tipoMime, String legenda) {
        if (cabeMais()) {
            fotos.add(new Foto(conteudo, tipoMime, legenda));
        }
    }

    List<Foto> fotos() {
        return List.copyOf(fotos);
    }
}

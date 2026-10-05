package com.empresax.sistema.pdv.caixa.web;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Nomes para as respostas: a sessão guarda e-mails e o id do caixa físico; a tela mostra "Ana" e
 * "Caixa 01". Resolvidos em lote no controller (sem N+1).
 */
record NomesDoCaixa(Map<String, String> pessoas, Map<UUID, String> caixas) {

    private static final String SEM_CAIXA_NUMERADO = "Caixa sem número";

    NomesDoCaixa {
        pessoas = Map.copyOf(pessoas);
        caixas = Map.copyOf(caixas);
    }

    String pessoa(String email) {
        return pessoas.getOrDefault(email, email);
    }

    /** Sessões anteriores aos caixas numerados não têm caixa físico. */
    String caixa(Optional<UUID> pontoCaixaId) {
        return pontoCaixaId.map(id -> caixas.getOrDefault(id, SEM_CAIXA_NUMERADO)).orElse(SEM_CAIXA_NUMERADO);
    }
}

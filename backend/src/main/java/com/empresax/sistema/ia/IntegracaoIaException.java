package com.empresax.sistema.ia;

import com.empresax.sistema.common.domain.IntegracaoExternaException;

public class IntegracaoIaException extends IntegracaoExternaException {

    public IntegracaoIaException(String mensagemTecnica) {
        super(mensagemTecnica);
    }

    public IntegracaoIaException(String mensagemTecnica, Throwable causa) {
        super(mensagemTecnica, causa);
    }

    @Override
    public String mensagemPublica() {
        return "Assistente de IA indisponível no momento.";
    }
}

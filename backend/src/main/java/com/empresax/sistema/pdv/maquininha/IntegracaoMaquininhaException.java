package com.empresax.sistema.pdv.maquininha;

import com.empresax.sistema.common.domain.IntegracaoExternaException;

public class IntegracaoMaquininhaException extends IntegracaoExternaException {

    public IntegracaoMaquininhaException(String mensagemTecnica) {
        super(mensagemTecnica);
    }

    public IntegracaoMaquininhaException(String mensagemTecnica, Throwable causa) {
        super(mensagemTecnica, causa);
    }

    @Override
    public String mensagemPublica() {
        return "Não foi possível falar com a maquininha. Confira se ela está ligada e conectada, ou use a contingência.";
    }
}

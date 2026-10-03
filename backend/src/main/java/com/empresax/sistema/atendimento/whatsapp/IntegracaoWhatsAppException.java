package com.empresax.sistema.atendimento.whatsapp;

import com.empresax.sistema.common.domain.IntegracaoExternaException;

public class IntegracaoWhatsAppException extends IntegracaoExternaException {

    public IntegracaoWhatsAppException(String mensagemTecnica) {
        super(mensagemTecnica);
    }

    public IntegracaoWhatsAppException(String mensagemTecnica, Throwable causa) {
        super(mensagemTecnica, causa);
    }

    @Override
    public String mensagemPublica() {
        return "WhatsApp não conectado: não foi possível enviar a mensagem. Verifique a instância e o token da W-API.";
    }
}

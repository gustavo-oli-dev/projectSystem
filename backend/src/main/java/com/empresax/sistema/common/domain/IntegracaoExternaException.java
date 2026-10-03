package com.empresax.sistema.common.domain;

/**
 * Falha ao falar com um serviço externo (pagamento, WhatsApp, IA). Separa a mensagem técnica
 * (vai pro log) da mensagem pública (vai pro cliente da API, sem detalhe interno).
 */
public abstract class IntegracaoExternaException extends RuntimeException {

    protected IntegracaoExternaException(String mensagemTecnica) {
        super(mensagemTecnica);
    }

    protected IntegracaoExternaException(String mensagemTecnica, Throwable causa) {
        super(mensagemTecnica, causa);
    }

    public abstract String mensagemPublica();
}

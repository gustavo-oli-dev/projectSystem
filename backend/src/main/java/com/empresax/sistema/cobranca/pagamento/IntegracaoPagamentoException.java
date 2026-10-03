package com.empresax.sistema.cobranca.pagamento;

import com.empresax.sistema.common.domain.IntegracaoExternaException;

/**
 * Falha de comunicação ou resposta de erro do provedor de pagamento. O cliente da nossa API recebe
 * só a mensagem pública — não precisa saber que o provedor é o Mercado Pago.
 */
public class IntegracaoPagamentoException extends IntegracaoExternaException {

    public IntegracaoPagamentoException(String mensagemTecnica) {
        super(mensagemTecnica);
    }

    public IntegracaoPagamentoException(String mensagemTecnica, Throwable causa) {
        super(mensagemTecnica, causa);
    }

    @Override
    public String mensagemPublica() {
        return "Provedor de pagamento indisponível ou com credenciais inválidas. Tente novamente mais tarde.";
    }
}

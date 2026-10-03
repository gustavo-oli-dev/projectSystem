package com.empresax.sistema.common.domain;

/**
 * O usuário está autenticado mas a regra de negócio não permite a ação (ex.: conceder uma permissão
 * que ele mesmo não tem). Mapeada para HTTP 403 pelo GlobalExceptionHandler.
 */
public class AcessoNegadoException extends RuntimeException {

    public AcessoNegadoException(String mensagem) {
        super(mensagem);
    }
}

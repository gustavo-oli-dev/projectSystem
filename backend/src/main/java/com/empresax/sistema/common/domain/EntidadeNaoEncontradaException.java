package com.empresax.sistema.common.domain;

/**
 * Lançada quando uma entidade buscada por identificador não existe.
 * Mapeada para HTTP 404 pelo GlobalExceptionHandler.
 */
public class EntidadeNaoEncontradaException extends RuntimeException {

    public EntidadeNaoEncontradaException(String mensagem) {
        super(mensagem);
    }
}

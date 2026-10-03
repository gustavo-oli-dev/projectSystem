package com.empresax.sistema.common.domain;

/**
 * Lançada quando uma regra de negócio é violada ao criar ou alterar uma entidade
 * ou value object (estado inválido). Mapeada para HTTP 422 pelo GlobalExceptionHandler.
 */
public class DomainException extends RuntimeException {

    public DomainException(String mensagem) {
        super(mensagem);
    }
}

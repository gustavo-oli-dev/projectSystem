package com.empresax.sistema.shared.armazenamento;

public class ArmazenamentoObjetosException extends RuntimeException {

    public ArmazenamentoObjetosException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}

package com.empresax.sistema.shared.documento;

/**
 * Documento de identificação de um Cliente: CPF (pessoa física) ou CNPJ (pessoa jurídica).
 */
public sealed interface Documento permits Cpf, Cnpj {

    String valor();

    /**
     * Decide entre Cpf e Cnpj pelo tamanho do valor normalizado — único ponto de decisão,
     * evitando if/switch repetidos pelo resto do sistema (RF04).
     */
    static Documento criar(String valor) {
        String normalizado = valor == null ? "" : valor.replaceAll("[./-]", "");
        if (normalizado.length() == 11) {
            return new Cpf(normalizado);
        }
        return new Cnpj(normalizado);
    }
}

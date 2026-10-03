package com.empresax.sistema.shared.documento;

import com.empresax.sistema.common.domain.DomainException;

import java.util.regex.Pattern;

/**
 * CNPJ no formato numérico clássico (14 dígitos) ou no formato alfanumérico instituído pela
 * Receita Federal (IN RFB 2.229/2024, válido nacionalmente desde julho de 2026): 12 caracteres
 * alfanuméricos + 2 dígitos verificadores numéricos.
 *
 * NOTA: o dígito verificador do formato alfanumérico é calculado convertendo cada caractere para
 * seu código ASCII menos 48 (dígitos '0'-'9' viram 0-9, letras 'A'-'Z' viram 17-42), com os
 * mesmos pesos do CNPJ numérico clássico — conforme a especificação pública da Receita Federal.
 * Conferir esta implementação contra o validador oficial antes de ir para produção.
 */
public record Cnpj(String valor) implements Documento {

    private static final Pattern FORMATO_VALIDO = Pattern.compile("[0-9A-Z]{12}\\d{2}");
    private static final int[] PESOS_PRIMEIRO_DIGITO = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
    private static final int[] PESOS_SEGUNDO_DIGITO = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};

    public Cnpj {
        valor = normalizar(valor);
        if (!FORMATO_VALIDO.matcher(valor).matches()) {
            throw new DomainException("CNPJ deve ter 14 caracteres: 12 alfanuméricos e 2 dígitos verificadores");
        }
        if (todosCaracteresIguais(valor)) {
            throw new DomainException("CNPJ inválido");
        }
        if (!digitosVerificadoresValidos(valor)) {
            throw new DomainException("CNPJ inválido");
        }
    }

    private static String normalizar(String entrada) {
        if (entrada == null) {
            throw new DomainException("CNPJ é obrigatório");
        }
        return entrada.replaceAll("[./-]", "").toUpperCase();
    }

    private static boolean todosCaracteresIguais(String valor) {
        return valor.chars().distinct().count() == 1;
    }

    private static boolean digitosVerificadoresValidos(String cnpj) {
        String raiz = cnpj.substring(0, 12);
        int primeiroDigito = calcularDigitoVerificador(raiz, PESOS_PRIMEIRO_DIGITO);
        String raizComPrimeiroDigito = raiz + primeiroDigito;
        int segundoDigito = calcularDigitoVerificador(raizComPrimeiroDigito, PESOS_SEGUNDO_DIGITO);
        return cnpj.equals(raiz + primeiroDigito + segundoDigito);
    }

    private static int calcularDigitoVerificador(String base, int[] pesos) {
        int soma = 0;
        for (int indice = 0; indice < base.length(); indice++) {
            soma += valorNumericoDoCaractere(base.charAt(indice)) * pesos[indice];
        }
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }

    private static int valorNumericoDoCaractere(char caractere) {
        return caractere - '0';
    }
}

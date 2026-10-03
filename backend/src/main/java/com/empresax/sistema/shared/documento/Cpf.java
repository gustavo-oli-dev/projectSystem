package com.empresax.sistema.shared.documento;

import com.empresax.sistema.common.domain.DomainException;

import java.util.regex.Pattern;

public record Cpf(String valor) implements Documento {

    private static final Pattern APENAS_DIGITOS = Pattern.compile("\\d{11}");

    public Cpf {
        valor = somenteDigitos(valor);
        if (!APENAS_DIGITOS.matcher(valor).matches()) {
            throw new DomainException("CPF deve conter 11 dígitos numéricos");
        }
        if (todosDigitosIguais(valor)) {
            throw new DomainException("CPF inválido");
        }
        if (!digitosVerificadoresValidos(valor)) {
            throw new DomainException("CPF inválido");
        }
    }

    private static String somenteDigitos(String entrada) {
        if (entrada == null) {
            throw new DomainException("CPF é obrigatório");
        }
        return entrada.replaceAll("\\D", "");
    }

    private static boolean todosDigitosIguais(String digitos) {
        return digitos.chars().distinct().count() == 1;
    }

    private static boolean digitosVerificadoresValidos(String digitos) {
        int primeiroDigito = calcularDigitoVerificador(digitos.substring(0, 9), 10);
        int segundoDigito = calcularDigitoVerificador(digitos.substring(0, 9) + primeiroDigito, 11);
        return digitos.equals(digitos.substring(0, 9) + primeiroDigito + segundoDigito);
    }

    private static int calcularDigitoVerificador(String base, int pesoInicial) {
        int soma = 0;
        int peso = pesoInicial;
        for (char caractere : base.toCharArray()) {
            soma += Character.getNumericValue(caractere) * peso;
            peso--;
        }
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }
}

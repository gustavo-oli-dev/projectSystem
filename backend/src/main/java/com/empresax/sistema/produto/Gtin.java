package com.empresax.sistema.produto;

import com.empresax.sistema.common.domain.DomainException;

import java.util.regex.Pattern;

/** Código de barras EAN/GTIN (8, 12, 13 ou 14 dígitos), com o dígito verificador GS1 conferido. */
public final class Gtin {

    private static final Pattern GTIN_VALIDO = Pattern.compile("\\d{8}|\\d{12,14}");
    private static final int PESO_GTIN_IMPAR = 3;
    private static final int MODULO_GTIN = 10;

    private Gtin() {
    }

    /** Vazio = sem código (devolve null). Inválido = erro (pega erro de digitação ou de leitura). */
    public static String validarOpcional(String codigoBarras) {
        if (codigoBarras == null || codigoBarras.isBlank()) {
            return null;
        }
        String codigo = codigoBarras.trim();
        if (!GTIN_VALIDO.matcher(codigo).matches() || !digitoVerificadorConfere(codigo)) {
            throw new DomainException("Código de barras inválido (EAN/GTIN de 8, 12, 13 ou 14 dígitos)");
        }
        return codigo;
    }

    private static boolean digitoVerificadorConfere(String codigo) {
        int soma = 0;
        int ultimo = codigo.length() - 1;
        for (int posicao = 0; posicao < ultimo; posicao++) {
            int digito = codigo.charAt(ultimo - 1 - posicao) - '0';
            soma += posicao % 2 == 0 ? digito * PESO_GTIN_IMPAR : digito;
        }
        int verificadorEsperado = (MODULO_GTIN - soma % MODULO_GTIN) % MODULO_GTIN;
        return verificadorEsperado == codigo.charAt(ultimo) - '0';
    }
}

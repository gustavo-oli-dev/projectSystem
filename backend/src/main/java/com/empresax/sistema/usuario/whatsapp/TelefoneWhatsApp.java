package com.empresax.sistema.usuario.whatsapp;

import com.empresax.sistema.common.domain.DomainException;

import java.util.regex.Pattern;

/**
 * Número de WhatsApp no formato que a W-API usa como remetente: só dígitos, com DDI 55 e DDD
 * (ex.: 5585988887777). Aceita a digitação comum do usuário — "(85) 98888-7777" vira 5585988887777.
 */
public record TelefoneWhatsApp(String numero) {

    private static final String DDI_BRASIL = "55";
    private static final Pattern FORMATO_VALIDO = Pattern.compile("^55\\d{10,11}$");
    private static final int TAMANHO_SEM_DDI_MINIMO = 10;
    private static final int TAMANHO_SEM_DDI_MAXIMO = 11;

    public TelefoneWhatsApp {
        if (numero == null || !FORMATO_VALIDO.matcher(numero).matches()) {
            throw new DomainException("Número de WhatsApp inválido. Use DDD + número, ex.: (85) 98888-7777");
        }
    }

    public static TelefoneWhatsApp de(String digitado) {
        String digitos = digitado == null ? "" : digitado.replaceAll("\\D", "");
        boolean semDdi = digitos.length() >= TAMANHO_SEM_DDI_MINIMO && digitos.length() <= TAMANHO_SEM_DDI_MAXIMO;
        return new TelefoneWhatsApp(semDdi ? DDI_BRASIL + digitos : digitos);
    }
}

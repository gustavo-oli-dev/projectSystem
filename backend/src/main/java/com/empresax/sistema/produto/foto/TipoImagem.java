package com.empresax.sistema.produto.foto;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Optional;

/**
 * Formatos aceitos para foto de produto. O tipo é detectado pelos primeiros bytes do arquivo
 * ("assinatura"), nunca pelo nome nem pelo Content-Type enviado — ambos podem mentir.
 */
public enum TipoImagem {
    JPEG("image/jpeg"),
    PNG("image/png"),
    WEBP("image/webp");

    private static final byte[] ASSINATURA_JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] ASSINATURA_PNG = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'};
    private static final byte[] RIFF = "RIFF".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] WEBP_MARCA = "WEBP".getBytes(StandardCharsets.US_ASCII);
    private static final int INICIO_MARCA_WEBP = 8;

    private final String tipoMime;

    TipoImagem(String tipoMime) {
        this.tipoMime = tipoMime;
    }

    public String tipoMime() {
        return tipoMime;
    }

    public static Optional<TipoImagem> detectar(byte[] conteudo) {
        if (conteudo == null) {
            return Optional.empty();
        }
        if (comecaCom(conteudo, ASSINATURA_JPEG, 0)) {
            return Optional.of(JPEG);
        }
        if (comecaCom(conteudo, ASSINATURA_PNG, 0)) {
            return Optional.of(PNG);
        }
        if (comecaCom(conteudo, RIFF, 0) && comecaCom(conteudo, WEBP_MARCA, INICIO_MARCA_WEBP)) {
            return Optional.of(WEBP);
        }
        return Optional.empty();
    }

    private static boolean comecaCom(byte[] conteudo, byte[] assinatura, int deslocamento) {
        if (conteudo.length < deslocamento + assinatura.length) {
            return false;
        }
        return Arrays.equals(conteudo, deslocamento, deslocamento + assinatura.length, assinatura, 0, assinatura.length);
    }
}

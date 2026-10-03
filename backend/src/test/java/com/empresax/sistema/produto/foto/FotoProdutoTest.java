package com.empresax.sistema.produto.foto;

import com.empresax.sistema.common.domain.DomainException;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FotoProdutoTest {

    private static final UUID PRODUTO = UUID.randomUUID();
    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0x10};
    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n', 0, 0};

    @Test
    void detectaJpegPngEWebpPeloConteudo() {
        byte[] webp = "RIFF\0\0\0\0WEBPVP8 ".getBytes(StandardCharsets.ISO_8859_1);

        assertThat(TipoImagem.detectar(JPEG)).contains(TipoImagem.JPEG);
        assertThat(TipoImagem.detectar(PNG)).contains(TipoImagem.PNG);
        assertThat(TipoImagem.detectar(webp)).contains(TipoImagem.WEBP);
    }

    @Test
    void arquivoQueNaoEImagemEhRecusadoMesmoComNomeDeFoto() {
        byte[] scriptDisfarcado = "<script>alert(1)</script>".getBytes(StandardCharsets.UTF_8);

        assertThatThrownBy(() -> new FotoProduto(PRODUTO, scriptDisfarcado, 0))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("Formato");
    }

    @Test
    void fotoAcimaDeCincoMegaEhRecusada() {
        byte[] grande = new byte[FotoProduto.TAMANHO_MAXIMO_BYTES + 1];
        System.arraycopy(JPEG, 0, grande, 0, JPEG.length);

        assertThatThrownBy(() -> new FotoProduto(PRODUTO, grande, 0))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("5 MB");
    }

    @Test
    void fotoValidaGuardaOTipoDetectado() {
        FotoProduto foto = new FotoProduto(PRODUTO, PNG, 0);

        assertThat(foto.tipo()).isEqualTo(TipoImagem.PNG);
        assertThat(foto.conteudo()).isEqualTo(PNG);
    }
}

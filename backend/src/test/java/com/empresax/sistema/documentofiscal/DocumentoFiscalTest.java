package com.empresax.sistema.documentofiscal;

import com.empresax.sistema.common.domain.DomainException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DocumentoFiscalTest {

    @Test
    void nasceComStatusPendente() {
        DocumentoFiscal documento = new DocumentoFiscal(UUID.randomUUID(), TipoDocumentoFiscal.NFE);

        assertThat(documento.status()).isEqualTo(StatusDocumentoFiscal.PENDENTE);
    }

    @Test
    void autorizarMudaStatusEGuardaXmlEProtocolo() {
        DocumentoFiscal documento = new DocumentoFiscal(UUID.randomUUID(), TipoDocumentoFiscal.NFE);

        documento.autorizar("<xml/>", "123456789");

        assertThat(documento.status()).isEqualTo(StatusDocumentoFiscal.AUTORIZADO);
        assertThat(documento.xmlAutorizado()).isEqualTo("<xml/>");
        assertThat(documento.protocolo()).isEqualTo("123456789");
    }

    @Test
    void naoPermiteAutorizarDocumentoJaProcessado() {
        DocumentoFiscal documento = new DocumentoFiscal(UUID.randomUUID(), TipoDocumentoFiscal.NFE);
        documento.rejeitar("SEFAZ indisponível");

        assertThatThrownBy(() -> documento.autorizar("<xml/>", "123")).isInstanceOf(DomainException.class);
    }

    @Test
    void naoPermiteCancelarDocumentoNaoAutorizado() {
        DocumentoFiscal documento = new DocumentoFiscal(UUID.randomUUID(), TipoDocumentoFiscal.NFSE);

        assertThatThrownBy(documento::cancelar).isInstanceOf(DomainException.class);
    }

    @Test
    void cancelarDocumentoAutorizadoMudaStatus() {
        DocumentoFiscal documento = new DocumentoFiscal(UUID.randomUUID(), TipoDocumentoFiscal.NFSE);
        documento.autorizar("<xml/>", "123456789");

        documento.cancelar();

        assertThat(documento.status()).isEqualTo(StatusDocumentoFiscal.CANCELADO);
    }
}

package com.empresax.sistema.documentofiscal.web;

import com.empresax.sistema.documentofiscal.DocumentoFiscal;
import com.empresax.sistema.documentofiscal.StatusDocumentoFiscal;
import com.empresax.sistema.documentofiscal.TipoDocumentoFiscal;

import java.time.Instant;
import java.util.UUID;

/** O XML autorizado não vai na listagem — é grande e só interessa no detalhe/download. */
public record DocumentoFiscalResponse(
        UUID id,
        UUID pedidoId,
        TipoDocumentoFiscal tipo,
        StatusDocumentoFiscal status,
        String protocolo,
        String motivoRejeicao,
        Instant criadoEm,
        Instant atualizadoEm
) {

    public static DocumentoFiscalResponse de(DocumentoFiscal documento) {
        return new DocumentoFiscalResponse(
                documento.id(),
                documento.pedidoId(),
                documento.tipo(),
                documento.status(),
                documento.protocolo(),
                documento.motivoRejeicao(),
                documento.criadoEm(),
                documento.atualizadoEm()
        );
    }
}

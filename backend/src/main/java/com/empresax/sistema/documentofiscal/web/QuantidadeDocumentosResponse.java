package com.empresax.sistema.documentofiscal.web;

import com.empresax.sistema.documentofiscal.QuantidadePorStatus;
import com.empresax.sistema.documentofiscal.StatusDocumentoFiscal;

public record QuantidadeDocumentosResponse(StatusDocumentoFiscal status, long quantidade) {

    public static QuantidadeDocumentosResponse de(QuantidadePorStatus contagem) {
        return new QuantidadeDocumentosResponse(contagem.status(), contagem.quantidade());
    }
}

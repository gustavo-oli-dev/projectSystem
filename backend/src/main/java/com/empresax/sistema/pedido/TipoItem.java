package com.empresax.sistema.pedido;

import com.empresax.sistema.documentofiscal.TipoDocumentoFiscal;

/**
 * Cada tipo de item sabe qual documento fiscal exige: produto gera NF-e, serviço gera NFS-e
 * (RF03). Fica no próprio enum para nenhum service precisar de if/switch sobre o tipo.
 */
public enum TipoItem {
    PRODUTO(TipoDocumentoFiscal.NFE),
    SERVICO(TipoDocumentoFiscal.NFSE);

    private final TipoDocumentoFiscal documentoFiscal;

    TipoItem(TipoDocumentoFiscal documentoFiscal) {
        this.documentoFiscal = documentoFiscal;
    }

    public TipoDocumentoFiscal documentoFiscal() {
        return documentoFiscal;
    }
}

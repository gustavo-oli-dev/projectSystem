package com.empresax.sistema.documentofiscal;

/** Quantos documentos fiscais existem em um status (contagem feita no banco). */
public record QuantidadePorStatus(StatusDocumentoFiscal status, long quantidade) {
}

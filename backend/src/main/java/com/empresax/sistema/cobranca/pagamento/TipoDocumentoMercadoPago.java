package com.empresax.sistema.cobranca.pagamento;

import com.empresax.sistema.shared.documento.Cnpj;
import com.empresax.sistema.shared.documento.Documento;

/**
 * Código de tipo de documento que a API do Mercado Pago espera em payer.identification.type.
 */
enum TipoDocumentoMercadoPago {
    CPF("CPF"),
    CNPJ("CNPJ");

    private final String codigo;

    TipoDocumentoMercadoPago(String codigo) {
        this.codigo = codigo;
    }

    String codigo() {
        return codigo;
    }

    static TipoDocumentoMercadoPago de(Documento documento) {
        return documento instanceof Cnpj ? CNPJ : CPF;
    }
}

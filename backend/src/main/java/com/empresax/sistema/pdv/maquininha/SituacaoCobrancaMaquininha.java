package com.empresax.sistema.pdv.maquininha;

import com.empresax.sistema.pdv.BandeiraCartao;

/** Resultado consultado na maquininha. Bandeira, autorização e pagamento só vêm quando aprovada. */
public record SituacaoCobrancaMaquininha(
        StatusCobrancaMaquininha status,
        BandeiraCartao bandeira,
        String codigoAutorizacao,
        String idPagamento
) {

    public static SituacaoCobrancaMaquininha semResultado(StatusCobrancaMaquininha status) {
        return new SituacaoCobrancaMaquininha(status, null, null, null);
    }
}

package com.empresax.sistema.documentofiscal.web;

import com.empresax.sistema.documentofiscal.ConfiguracaoFiscal;

public record ConfiguracaoFiscalResponse(
        String ambiente,
        boolean certificadoA1Configurado,
        boolean regimeTributarioDefinido,
        boolean provedorNfseConfigurado,
        boolean emissaoHabilitada
) {

    public static ConfiguracaoFiscalResponse de(ConfiguracaoFiscal configuracao) {
        return new ConfiguracaoFiscalResponse(
                configuracao.ambiente(),
                configuracao.certificadoA1Configurado(),
                configuracao.regimeTributarioDefinido(),
                configuracao.provedorNfseConfigurado(),
                configuracao.emissaoHabilitada()
        );
    }
}

package com.empresax.sistema.documentofiscal;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Diz o que está (ou não) configurado para a emissão fiscal de verdade. Expõe só booleanos:
 * nunca o caminho do certificado nem o token do provedor (segredos).
 */
@Component
public class ConfiguracaoFiscal {

    private final String ambiente;
    private final String caminhoCertificadoA1;
    private final String regimeTributario;
    private final String tokenProvedorNfse;

    public ConfiguracaoFiscal(
            @Value("${fiscal.ambiente}") String ambiente,
            @Value("${fiscal.certificado-a1-caminho}") String caminhoCertificadoA1,
            @Value("${fiscal.regime-tributario}") String regimeTributario,
            @Value("${fiscal.provedor-nfse-token}") String tokenProvedorNfse
    ) {
        this.ambiente = ambiente;
        this.caminhoCertificadoA1 = caminhoCertificadoA1;
        this.regimeTributario = regimeTributario;
        this.tokenProvedorNfse = tokenProvedorNfse;
    }

    public String ambiente() {
        return ambiente;
    }

    public boolean certificadoA1Configurado() {
        return !caminhoCertificadoA1.isBlank();
    }

    public boolean regimeTributarioDefinido() {
        return !regimeTributario.isBlank();
    }

    public boolean provedorNfseConfigurado() {
        return !tokenProvedorNfse.isBlank();
    }

    public boolean emissaoHabilitada() {
        return certificadoA1Configurado() && regimeTributarioDefinido() && provedorNfseConfigurado();
    }
}

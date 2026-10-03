package com.empresax.sistema.cobranca.web;

import com.empresax.sistema.cobranca.Cobranca;
import com.empresax.sistema.cobranca.CobrancaCriada;
import com.empresax.sistema.cobranca.MeioCobranca;
import com.empresax.sistema.cobranca.StatusCobranca;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Dados do Pix (copia e cola, imagem do QR) e do boleto só vêm preenchidos na resposta de criação —
 * não são persistidos (ver CobrancaCriada). Uma consulta posterior (GET) não os repete.
 */
public record CobrancaResponse(
        UUID id,
        UUID pedidoId,
        MeioCobranca meio,
        BigDecimal valor,
        StatusCobranca status,
        String qrCodeCopiaECola,
        String qrCodeImagemBase64,
        String linhaDigitavelBoleto,
        String urlBoleto,
        Instant criadoEm
) {

    public static CobrancaResponse de(Cobranca cobranca) {
        return new CobrancaResponse(
                cobranca.id(), cobranca.pedidoId(), cobranca.meio(), cobranca.valor().valor(), cobranca.status(),
                null, null, null, null, cobranca.criadoEm());
    }

    public static CobrancaResponse de(CobrancaCriada cobrancaCriada) {
        Cobranca cobranca = cobrancaCriada.cobranca();
        var dados = cobrancaCriada.dadosExternos();
        return new CobrancaResponse(
                cobranca.id(), cobranca.pedidoId(), cobranca.meio(), cobranca.valor().valor(), cobranca.status(),
                dados.qrCodeCopiaECola(), dados.qrCodeImagemBase64(), dados.linhaDigitavelBoleto(), dados.urlBoleto(),
                cobranca.criadoEm());
    }
}

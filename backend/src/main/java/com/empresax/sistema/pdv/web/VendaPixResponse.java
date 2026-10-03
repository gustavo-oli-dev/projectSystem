package com.empresax.sistema.pdv.web;

import com.empresax.sistema.cobranca.CobrancaCriada;

import java.math.BigDecimal;
import java.util.UUID;

/** O que o caixa precisa para mostrar o QR: a imagem e o copia e cola (só existem na criação). */
public record VendaPixResponse(
        UUID pedidoId,
        BigDecimal total,
        String qrCodeCopiaECola,
        String qrCodeImagemBase64
) {

    public static VendaPixResponse de(CobrancaCriada criada) {
        return new VendaPixResponse(
                criada.cobranca().pedidoId(),
                criada.cobranca().valor().valor(),
                criada.dadosExternos().qrCodeCopiaECola(),
                criada.dadosExternos().qrCodeImagemBase64());
    }
}

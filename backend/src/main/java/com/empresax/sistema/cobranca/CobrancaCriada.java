package com.empresax.sistema.cobranca;

import com.empresax.sistema.cobranca.pagamento.DadosCobrancaExterna;

/**
 * Resultado de criar uma cobrança: a entidade persistida + os dados que só existem no momento da
 * criação (QR code do Pix, linha digitável do boleto) — não são guardados no banco, só devolvidos
 * uma vez ao chamador (ver nota em CobrancaResponse).
 */
public record CobrancaCriada(Cobranca cobranca, DadosCobrancaExterna dadosExternos) {
}

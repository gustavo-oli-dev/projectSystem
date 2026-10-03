package com.empresax.sistema.cobranca.pagamento;

/**
 * Dados devolvidos pelo provedor de pagamento ao criar uma cobrança. qrCodeCopiaECola só vem
 * preenchido para Pix; linhaDigitavelBoleto/urlBoleto só para boleto.
 */
public record DadosCobrancaExterna(
        String referenciaExterna,
        String qrCodeCopiaECola,
        String linhaDigitavelBoleto,
        String urlBoleto
) {
}

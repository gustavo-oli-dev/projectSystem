package com.empresax.sistema.cobranca.pagamento;

/**
 * Dados devolvidos pelo provedor de pagamento ao criar uma cobrança. Os campos de Pix (copia e cola
 * e imagem do QR em base64) só vêm preenchidos para Pix; linhaDigitavelBoleto/urlBoleto só para boleto.
 */
public record DadosCobrancaExterna(
        String referenciaExterna,
        String qrCodeCopiaECola,
        String qrCodeImagemBase64,
        String linhaDigitavelBoleto,
        String urlBoleto
) {
}

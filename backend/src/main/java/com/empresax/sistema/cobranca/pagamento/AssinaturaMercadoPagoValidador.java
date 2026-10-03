package com.empresax.sistema.cobranca.pagamento;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;

/**
 * Valida a assinatura do webhook do Mercado Pago (cabeçalho x-signature: "ts=...,v1=...",
 * HMAC-SHA256 sobre o manifesto "id:{data.id};request-id:{x-request-id};ts:{ts};").
 *
 * NOTA: formato documentado publicamente pelo Mercado Pago, mas nunca exercitado contra uma
 * notificação real — conferir antes de produção (ver PENDENCIAS.md).
 */
@Component
public class AssinaturaMercadoPagoValidador {

    private static final String ALGORITMO_HMAC = "HmacSHA256";

    private final String segredoWebhook;

    public AssinaturaMercadoPagoValidador(@Value("${pagamento.mercadopago.segredo-webhook}") String segredoWebhook) {
        this.segredoWebhook = segredoWebhook;
    }

    public boolean valida(String cabecalhoAssinatura, String idRequisicao, String dataId) {
        if (cabecalhoAssinatura == null || idRequisicao == null || dataId == null) {
            return false;
        }

        Map<String, String> partes = extrairPartes(cabecalhoAssinatura);
        String timestamp = partes.get("ts");
        String assinaturaRecebida = partes.get("v1");
        if (timestamp == null || assinaturaRecebida == null) {
            return false;
        }

        String manifesto = "id:" + dataId + ";request-id:" + idRequisicao + ";ts:" + timestamp + ";";
        return calcularHmac(manifesto).equals(assinaturaRecebida);
    }

    private static Map<String, String> extrairPartes(String cabecalhoAssinatura) {
        return java.util.Arrays.stream(cabecalhoAssinatura.split(","))
                .map(parte -> parte.split("=", 2))
                .filter(parte -> parte.length == 2)
                .collect(java.util.stream.Collectors.toMap(parte -> parte[0].trim(), parte -> parte[1].trim()));
    }

    String calcularHmac(String manifesto) {
        try {
            Mac mac = Mac.getInstance(ALGORITMO_HMAC);
            mac.init(new SecretKeySpec(segredoWebhook.getBytes(StandardCharsets.UTF_8), ALGORITMO_HMAC));
            byte[] hash = mac.doFinal(manifesto.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException | java.security.InvalidKeyException excecao) {
            throw new IntegracaoPagamentoException("Falha ao calcular assinatura do webhook", excecao);
        }
    }
}

package com.empresax.sistema.pdv.maquininha;

import com.empresax.sistema.pdv.BandeiraCartao;
import com.empresax.sistema.pdv.FormaPagamentoPresencial;
import com.empresax.sistema.shared.dinheiro.Dinheiro;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

/**
 * Adapter da maquininha Mercado Pago Point (D20), pela "Point Integration API": cria uma intenção
 * de pagamento no aparelho, consulta o estado e lê bandeira/autorização no pagamento resultante.
 *
 * NOTA: segue a documentação pública, mas nunca foi exercitado contra a API real (falta token e
 * aparelho Point vinculado). Caminhos da resposta e estados precisam ser conferidos no primeiro
 * teste (ver PENDENCIAS.md). O Mercado Pago também oferece a API "Orders" para Point; se a conta
 * usar só ela, este adapter precisa ser adaptado.
 */
@Component
public class MercadoPagoPointMaquininha implements Maquininha {

    private static final String URL_BASE = "https://api.mercadopago.com";
    private static final int STATUS_ERRO_MINIMO = 400;
    private static final BigDecimal CENTAVOS_POR_REAL = BigDecimal.valueOf(100);
    private static final String DESCRICAO = "Venda no balcão";
    private static final Map<String, BandeiraCartao> BANDEIRAS = Map.of(
            "visa", BandeiraCartao.VISA,
            "debvisa", BandeiraCartao.VISA,
            "master", BandeiraCartao.MASTERCARD,
            "debmaster", BandeiraCartao.MASTERCARD,
            "elo", BandeiraCartao.ELO,
            "debelo", BandeiraCartao.ELO,
            "amex", BandeiraCartao.AMERICAN_EXPRESS,
            "hipercard", BandeiraCartao.HIPERCARD);

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String tokenAcesso;
    private final String idDispositivo;

    public MercadoPagoPointMaquininha(
            @Value("${pagamento.mercadopago.token-acesso}") String tokenAcesso,
            @Value("${pagamento.mercadopago.point-dispositivo}") String idDispositivo
    ) {
        this.tokenAcesso = tokenAcesso;
        this.idDispositivo = idDispositivo;
    }

    /** POST /point/integration-api/devices/{id}/payment-intents — valor em centavos. */
    @Override
    public String enviarCobranca(Dinheiro valor, FormaPagamentoPresencial forma, UUID referenciaVenda) {
        if (idDispositivo.isBlank()) {
            throw new IntegracaoMaquininhaException("Maquininha Point não configurada (MERCADOPAGO_POINT_DISPOSITIVO)");
        }
        Map<String, Object> corpo = Map.of(
                "amount", valor.valor().multiply(CENTAVOS_POR_REAL).intValueExact(),
                "description", DESCRICAO,
                "payment", Map.of("type", forma == FormaPagamentoPresencial.CARTAO_DEBITO ? "debit_card" : "credit_card"),
                "additional_info", Map.of("external_reference", referenciaVenda.toString(), "print_on_terminal", true));
        HttpRequest requisicao = autenticada(caminhoDispositivo() + "/payment-intents")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(escreverJson(corpo)))
                .build();
        return enviarELer(requisicao).path("id").asText();
    }

    /** GET da intenção; terminada → GET /v1/payments/{id} para status, bandeira e autorização. */
    @Override
    public SituacaoCobrancaMaquininha consultar(String idCobranca) {
        JsonNode intencao = enviarELer(autenticada("/point/integration-api/payment-intents/" + idCobranca).GET().build());
        String estado = intencao.path("state").asText("");
        return switch (estado) {
            case "FINISHED" -> lerPagamento(intencao.path("payment").path("id").asText());
            case "CANCELED", "ABANDONED" -> SituacaoCobrancaMaquininha.semResultado(StatusCobrancaMaquininha.CANCELADA);
            case "ERROR" -> SituacaoCobrancaMaquininha.semResultado(StatusCobrancaMaquininha.RECUSADA);
            default -> SituacaoCobrancaMaquininha.semResultado(StatusCobrancaMaquininha.AGUARDANDO);
        };
    }

    @Override
    public void cancelarCobranca(String idCobranca) {
        enviarELer(autenticada(caminhoDispositivo() + "/payment-intents/" + idCobranca).DELETE().build());
    }

    /** Pagamento feito na Point é um pagamento comum: estorna pelo mesmo endpoint de reembolso. */
    @Override
    public void estornar(String idPagamento, String chaveIdempotencia) {
        HttpRequest requisicao = autenticada("/v1/payments/" + idPagamento + "/refunds")
                .header("Content-Type", "application/json")
                .header("X-Idempotency-Key", chaveIdempotencia)
                .POST(HttpRequest.BodyPublishers.ofString("{}"))
                .build();
        enviarELer(requisicao);
    }

    private SituacaoCobrancaMaquininha lerPagamento(String idPagamento) {
        JsonNode pagamento = enviarELer(autenticada("/v1/payments/" + idPagamento).GET().build());
        if (!"approved".equals(pagamento.path("status").asText())) {
            return SituacaoCobrancaMaquininha.semResultado(StatusCobrancaMaquininha.RECUSADA);
        }
        BandeiraCartao bandeira = BANDEIRAS.getOrDefault(pagamento.path("payment_method_id").asText(""), BandeiraCartao.OUTRA);
        return new SituacaoCobrancaMaquininha(StatusCobrancaMaquininha.APROVADA, bandeira,
                pagamento.path("authorization_code").asText(null), idPagamento);
    }

    private String caminhoDispositivo() {
        return "/point/integration-api/devices/" + URLEncoder.encode(idDispositivo, StandardCharsets.UTF_8);
    }

    private HttpRequest.Builder autenticada(String caminho) {
        return HttpRequest.newBuilder().uri(URI.create(URL_BASE + caminho)).header("Authorization", "Bearer " + tokenAcesso);
    }

    private JsonNode enviarELer(HttpRequest requisicao) {
        try {
            HttpResponse<String> resposta = httpClient.send(requisicao, HttpResponse.BodyHandlers.ofString());
            if (resposta.statusCode() >= STATUS_ERRO_MINIMO) {
                throw new IntegracaoMaquininhaException("Mercado Pago Point respondeu HTTP " + resposta.statusCode());
            }
            return resposta.body().isBlank() ? objectMapper.createObjectNode() : objectMapper.readTree(resposta.body());
        } catch (IOException falha) {
            throw new IntegracaoMaquininhaException("Falha ao comunicar com o Mercado Pago Point", falha);
        } catch (InterruptedException falha) {
            Thread.currentThread().interrupt();
            throw new IntegracaoMaquininhaException("Comunicação com o Mercado Pago Point interrompida", falha);
        }
    }

    private String escreverJson(Object corpo) {
        try {
            return objectMapper.writeValueAsString(corpo);
        } catch (IOException falha) {
            throw new IntegracaoMaquininhaException("Falha ao montar a requisição para a maquininha", falha);
        }
    }
}

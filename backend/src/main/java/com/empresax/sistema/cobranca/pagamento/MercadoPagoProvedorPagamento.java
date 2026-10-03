package com.empresax.sistema.cobranca.pagamento;

import com.empresax.sistema.cliente.Cliente;
import com.empresax.sistema.cobranca.MeioCobranca;
import com.empresax.sistema.shared.dinheiro.Dinheiro;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;

/**
 * Adapter real para a API de Pagamentos do Mercado Pago (decisão D4 em DECISOES.md).
 *
 * NOTA: esta implementação segue a documentação pública da API (POST /v1/payments com
 * payment_method_id "pix" ou "bolbradesco"; GET /v1/payments/{id} para consulta de status), mas
 * nunca foi exercitada contra a API real — não há credenciais de teste disponíveis até agora.
 * O corpo da requisição e, principalmente, os caminhos usados para ler a resposta (QR code do
 * Pix, linha digitável do boleto) são uma estimativa e precisam ser conferidos com uma chamada de
 * teste real antes de produção (ver PENDENCIAS.md).
 */
@Service
public class MercadoPagoProvedorPagamento implements ProvedorPagamento {

    private static final String URL_BASE = "https://api.mercadopago.com";
    private static final String METODO_PIX = "pix";
    private static final String METODO_BOLETO = "bolbradesco";
    private static final int STATUS_ERRO_MINIMO = 400;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String tokenAcesso;

    public MercadoPagoProvedorPagamento(@Value("${pagamento.mercadopago.token-acesso}") String tokenAcesso) {
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
        this.tokenAcesso = tokenAcesso;
    }

    @Override
    public DadosCobrancaExterna criarCobranca(
            Dinheiro valor, String descricao, UUID referenciaPedido, MeioCobranca meio, Cliente cliente
    ) {
        String corpo = montarCorpoCriacao(valor, descricao, meio, cliente);
        HttpRequest requisicao = HttpRequest.newBuilder()
                .uri(URI.create(URL_BASE + "/v1/payments"))
                .header("Authorization", "Bearer " + tokenAcesso)
                .header("Content-Type", "application/json")
                .header("X-Idempotency-Key", referenciaPedido.toString())
                .POST(HttpRequest.BodyPublishers.ofString(corpo))
                .build();
        JsonNode resposta = enviarELer(requisicao);
        return interpretarCriacao(resposta, meio);
    }

    @Override
    public StatusPagamentoExterno consultarStatus(String referenciaExterna) {
        HttpRequest requisicao = HttpRequest.newBuilder()
                .uri(URI.create(URL_BASE + "/v1/payments/" + referenciaExterna))
                .header("Authorization", "Bearer " + tokenAcesso)
                .GET()
                .build();
        JsonNode resposta = enviarELer(requisicao);
        return interpretarStatus(resposta);
    }

    /** POST /v1/payments/{id}/refunds sem valor = reembolso total. Não exercitado contra a API real ainda. */
    @Override
    public void reembolsar(String referenciaExterna, String chaveIdempotencia) {
        HttpRequest requisicao = HttpRequest.newBuilder()
                .uri(URI.create(URL_BASE + "/v1/payments/" + referenciaExterna + "/refunds"))
                .header("Authorization", "Bearer " + tokenAcesso)
                .header("Content-Type", "application/json")
                .header("X-Idempotency-Key", chaveIdempotencia)
                .POST(HttpRequest.BodyPublishers.ofString("{}"))
                .build();
        enviarELer(requisicao);
    }

    /** PUT /v1/payments/{id} com status "cancelled" — vale para pagamento pendente. Não exercitado contra a API real ainda. */
    @Override
    public void cancelarCobranca(String referenciaExterna) {
        HttpRequest requisicao = HttpRequest.newBuilder()
                .uri(URI.create(URL_BASE + "/v1/payments/" + referenciaExterna))
                .header("Authorization", "Bearer " + tokenAcesso)
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString("{\"status\":\"cancelled\"}"))
                .build();
        enviarELer(requisicao);
    }

    private String montarCorpoCriacao(Dinheiro valor, String descricao, MeioCobranca meio, Cliente cliente) {
        String metodoPagamento = meio == MeioCobranca.PIX ? METODO_PIX : METODO_BOLETO;
        TipoDocumentoMercadoPago tipoDocumento = TipoDocumentoMercadoPago.de(cliente.documento());
        RequisicaoPagamento requisicao = new RequisicaoPagamento(
                valor.valor(),
                descricao,
                metodoPagamento,
                new Pagador(new IdentificacaoPagador(tipoDocumento.codigo(), cliente.documento().valor()))
        );
        return escreverJson(requisicao);
    }

    private DadosCobrancaExterna interpretarCriacao(JsonNode resposta, MeioCobranca meio) {
        String id = resposta.path("id").asText();
        if (meio == MeioCobranca.PIX) {
            JsonNode dadosTransacao = resposta.path("point_of_interaction").path("transaction_data");
            String copiaECola = dadosTransacao.path("qr_code").asText(null);
            return new DadosCobrancaExterna(id, copiaECola, null, null);
        }
        String linhaDigitavel = resposta.path("barcode").path("content").asText(null);
        String urlBoleto = resposta.path("transaction_details").path("external_resource_url").asText(null);
        return new DadosCobrancaExterna(id, null, linhaDigitavel, urlBoleto);
    }

    private StatusPagamentoExterno interpretarStatus(JsonNode resposta) {
        String status = resposta.path("status").asText("");
        return switch (status) {
            case "approved" -> StatusPagamentoExterno.APROVADO;
            case "pending", "in_process" -> StatusPagamentoExterno.PENDENTE;
            default -> StatusPagamentoExterno.REJEITADO;
        };
    }

    private JsonNode enviarELer(HttpRequest requisicao) {
        try {
            HttpResponse<String> resposta = httpClient.send(requisicao, HttpResponse.BodyHandlers.ofString());
            if (resposta.statusCode() >= STATUS_ERRO_MINIMO) {
                throw new IntegracaoPagamentoException("Mercado Pago respondeu HTTP " + resposta.statusCode());
            }
            return objectMapper.readTree(resposta.body());
        } catch (IOException excecao) {
            throw new IntegracaoPagamentoException("Falha ao comunicar com o Mercado Pago", excecao);
        } catch (InterruptedException excecao) {
            Thread.currentThread().interrupt();
            throw new IntegracaoPagamentoException("Comunicação com o Mercado Pago interrompida", excecao);
        }
    }

    private String escreverJson(Object corpo) {
        try {
            return objectMapper.writeValueAsString(corpo);
        } catch (IOException excecao) {
            throw new IntegracaoPagamentoException("Falha ao montar requisição para o Mercado Pago", excecao);
        }
    }

    private record RequisicaoPagamento(
            @JsonProperty("transaction_amount") java.math.BigDecimal valorTransacao,
            String description,
            @JsonProperty("payment_method_id") String metodoPagamentoId,
            Pagador payer
    ) {
    }

    private record Pagador(IdentificacaoPagador identification) {
    }

    private record IdentificacaoPagador(String type, String number) {
    }
}

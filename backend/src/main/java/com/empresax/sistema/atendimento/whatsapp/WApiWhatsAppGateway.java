package com.empresax.sistema.atendimento.whatsapp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

/**
 * Adapter para a W-API (API não oficial do WhatsApp, decisão D2 em DECISOES.md).
 * Endpoint e autenticação conferidos na especificação OpenAPI oficial (docs.w-api.app/openapi.json):
 * POST /v1/message/send-text?instanceId=..., Authorization: Bearer TOKEN, corpo {phone, message},
 * resposta com messageId.
 */
public class WApiWhatsAppGateway implements WhatsAppGateway {

    private static final int STATUS_ERRO_MINIMO = 400;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String urlBase;
    private final String instanceId;
    private final String token;
    private final String nomeCanal;

    /** Uma instância por número (ver ConfiguracaoWhatsApp); nomeCanal só identifica o número nos erros. */
    public WApiWhatsAppGateway(String urlBase, String instanceId, String token, String nomeCanal) {
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
        this.urlBase = urlBase;
        this.instanceId = instanceId;
        this.token = token;
        this.nomeCanal = nomeCanal;
    }

    @Override
    public String enviarTexto(String telefoneDestino, String texto) {
        if (instanceId.isBlank() || token.isBlank()) {
            throw new IntegracaoWhatsAppException(nomeCanal + ": instância ou token da W-API não configurados");
        }

        String corpo = escreverJson(Map.of("phone", telefoneDestino, "message", texto));
        HttpRequest requisicao = HttpRequest.newBuilder()
                .uri(URI.create(urlBase + "/v1/message/send-text?instanceId="
                        + URLEncoder.encode(instanceId, StandardCharsets.UTF_8)))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(corpo))
                .build();

        return enviarELer(requisicao).path("messageId").asText(null);
    }

    /**
     * POST /v1/message/send-image (mesma autenticação): {phone, image, caption}. A imagem vai em
     * base64 (data URI) porque as fotos ficam no nosso banco e não há endereço público para a W-API
     * buscar. Formato conferido na documentação, mas ainda não testado contra a API real (D43).
     */
    @Override
    public String enviarImagem(String telefoneDestino, byte[] imagem, String tipoMime, String legenda) {
        if (instanceId.isBlank() || token.isBlank()) {
            throw new IntegracaoWhatsAppException(nomeCanal + ": instância ou token da W-API não configurados");
        }
        String dataUri = "data:" + tipoMime + ";base64," + Base64.getEncoder().encodeToString(imagem);
        String corpo = escreverJson(Map.of("phone", telefoneDestino, "image", dataUri, "caption", legenda));
        HttpRequest requisicao = HttpRequest.newBuilder()
                .uri(URI.create(urlBase + "/v1/message/send-image?instanceId="
                        + URLEncoder.encode(instanceId, StandardCharsets.UTF_8)))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(corpo))
                .build();

        return enviarELer(requisicao).path("messageId").asText(null);
    }

    private JsonNode enviarELer(HttpRequest requisicao) {
        try {
            HttpResponse<String> resposta = httpClient.send(requisicao, HttpResponse.BodyHandlers.ofString());
            if (resposta.statusCode() >= STATUS_ERRO_MINIMO) {
                throw new IntegracaoWhatsAppException(nomeCanal + ": W-API respondeu HTTP " + resposta.statusCode());
            }
            return objectMapper.readTree(resposta.body());
        } catch (IOException excecao) {
            throw new IntegracaoWhatsAppException("Falha ao comunicar com a W-API", excecao);
        } catch (InterruptedException excecao) {
            Thread.currentThread().interrupt();
            throw new IntegracaoWhatsAppException("Comunicação com a W-API interrompida", excecao);
        }
    }

    private String escreverJson(Object corpo) {
        try {
            return objectMapper.writeValueAsString(corpo);
        } catch (IOException excecao) {
            throw new IntegracaoWhatsAppException("Falha ao montar requisição para a W-API", excecao);
        }
    }
}

package com.empresax.sistema.ia;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * Cliente da API de Mensagens da Claude. Não é um bean único de propósito: existem duas instâncias
 * com chaves diferentes (gestor e atendimento ao cliente — ver ConfiguracaoClaude), para que um
 * vazamento por engenharia de prompt num lado não comprometa o outro.
 */
public class ClaudeApiCliente {

    private static final String URL_BASE = "https://api.anthropic.com/v1/messages";
    private static final String VERSAO_API = "2023-06-01";
    private static final int MAX_TOKENS_RESPOSTA = 1024;
    private static final int STATUS_ERRO_MINIMO = 400;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String chaveApi;
    private final String modelo;

    public ClaudeApiCliente(String chaveApi, String modelo) {
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
        this.chaveApi = chaveApi;
        this.modelo = modelo;
    }

    public JsonNode enviarMensagens(String promptSistema, ArrayNode mensagens, ArrayNode ferramentas) {
        if (chaveApi.isBlank()) {
            throw new IntegracaoIaException("Chave da API da Claude não configurada");
        }

        ObjectNode corpo = objectMapper.createObjectNode();
        corpo.put("model", modelo);
        corpo.put("max_tokens", MAX_TOKENS_RESPOSTA);
        corpo.put("system", promptSistema);
        corpo.set("messages", mensagens);
        corpo.set("tools", ferramentas);

        HttpRequest requisicao = HttpRequest.newBuilder()
                .uri(URI.create(URL_BASE))
                .header("x-api-key", chaveApi)
                .header("anthropic-version", VERSAO_API)
                .header("content-type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(corpo.toString()))
                .build();

        return enviarELer(requisicao);
    }

    private JsonNode enviarELer(HttpRequest requisicao) {
        try {
            HttpResponse<String> resposta = httpClient.send(requisicao, HttpResponse.BodyHandlers.ofString());
            if (resposta.statusCode() >= STATUS_ERRO_MINIMO) {
                throw new IntegracaoIaException("API da Claude respondeu HTTP " + resposta.statusCode());
            }
            return objectMapper.readTree(resposta.body());
        } catch (IOException excecao) {
            throw new IntegracaoIaException("Falha ao comunicar com a API da Claude", excecao);
        } catch (InterruptedException excecao) {
            Thread.currentThread().interrupt();
            throw new IntegracaoIaException("Comunicação com a API da Claude interrompida", excecao);
        }
    }
}

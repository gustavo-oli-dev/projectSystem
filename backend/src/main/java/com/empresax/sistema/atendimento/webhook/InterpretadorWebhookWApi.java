package com.empresax.sistema.atendimento.webhook;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Extrai a mensagem do payload do webhook da W-API.
 *
 * NOTA: o formato do payload NÃO está na especificação OpenAPI da W-API. Os caminhos abaixo
 * (sender/chat, msgContent, fromMe, isGroup) são o formato usual dessa API, mas não verificados —
 * o payload bruto de cada evento fica guardado em eventos_webhook_whatsapp; conferir com a primeira
 * mensagem real e ajustar aqui se preciso (ver PENDENCIAS.md).
 */
@Component
public class InterpretadorWebhookWApi {

    private static final String SEPARADOR_JID = "@";

    public Optional<MensagemRecebida> interpretar(JsonNode payload) {
        if (payload.path("fromMe").asBoolean(false) || payload.path("isGroup").asBoolean(false)) {
            return Optional.empty();
        }

        String telefone = extrairTelefone(payload);
        String texto = extrairTexto(payload.path("msgContent"));
        if (telefone == null || texto == null || texto.isBlank()) {
            return Optional.empty();
        }

        String idExterno = payload.path("messageId").asText(null);
        return Optional.of(new MensagemRecebida(telefone, texto, idExterno));
    }

    private static String extrairTelefone(JsonNode payload) {
        String identificador = payload.path("sender").path("id").asText(null);
        if (identificador == null) {
            identificador = payload.path("chat").path("id").asText(null);
        }
        if (identificador == null) {
            return null;
        }
        return identificador.split(SEPARADOR_JID)[0];
    }

    private static String extrairTexto(JsonNode conteudo) {
        String textoSimples = conteudo.path("conversation").asText(null);
        if (textoSimples != null) {
            return textoSimples;
        }
        return conteudo.path("extendedTextMessage").path("text").asText(null);
    }
}

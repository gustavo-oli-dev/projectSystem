package com.empresax.sistema.atendimento.whatsapp;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Dois números de WhatsApp, cada um numa instância própria da W-API (D17): o da empresa atende
 * clientes; o interno atende só funcionários verificados com acesso ao assistente. Separar os
 * números evita que dado gerencial vaze para o canal de clientes. Sem bean "padrão" de propósito:
 * todo ponto de injeção precisa dizer qual número usa (@Qualifier).
 */
@Configuration
public class ConfiguracaoWhatsApp {

    public static final String ATENDIMENTO = "whatsAppAtendimento";
    public static final String ASSISTENTE = "whatsAppAssistente";

    @Bean(ATENDIMENTO)
    public WhatsAppGateway whatsAppAtendimento(
            @Value("${whatsapp.wapi.url-base}") String urlBase,
            @Value("${whatsapp.wapi.instance-id}") String instanceId,
            @Value("${whatsapp.wapi.token}") String token
    ) {
        return new WApiWhatsAppGateway(urlBase, instanceId, token, "WhatsApp de atendimento");
    }

    @Bean(ASSISTENTE)
    public WhatsAppGateway whatsAppAssistente(
            @Value("${whatsapp.wapi.url-base}") String urlBase,
            @Value("${assistente.whatsapp.instance-id}") String instanceId,
            @Value("${assistente.whatsapp.token}") String token
    ) {
        return new WApiWhatsAppGateway(urlBase, instanceId, token, "WhatsApp interno do assistente");
    }
}

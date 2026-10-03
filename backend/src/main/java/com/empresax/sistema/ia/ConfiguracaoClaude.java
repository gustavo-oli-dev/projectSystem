package com.empresax.sistema.ia;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Duas instâncias independentes da Claude, cada uma com a própria chave (decisão da análise
 * inicial do projeto): a do gestor (somente leitura do negócio) e a do atendimento ao cliente.
 */
@Configuration
public class ConfiguracaoClaude {

    public static final String CLAUDE_GESTOR = "claudeGestor";
    public static final String CLAUDE_ATENDIMENTO = "claudeAtendimento";

    @Bean(CLAUDE_GESTOR)
    public ClaudeApiCliente claudeGestor(
            @Value("${assistente.claude.chave-api}") String chaveApi,
            @Value("${assistente.claude.modelo}") String modelo
    ) {
        return new ClaudeApiCliente(chaveApi, modelo);
    }

    @Bean(CLAUDE_ATENDIMENTO)
    public ClaudeApiCliente claudeAtendimento(
            @Value("${atendimento.claude.chave-api}") String chaveApi,
            @Value("${atendimento.claude.modelo}") String modelo
    ) {
        return new ClaudeApiCliente(chaveApi, modelo);
    }
}

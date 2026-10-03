package com.empresax.sistema.relatorios;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Projeção deliberadamente simples da Cobranca para consumidores internos (assistente de IA,
 * painel) — entidades JPA não saem da camada de serviço.
 */
public record ResumoCobranca(UUID pedidoId, String meio, BigDecimal valor, Instant criadoEm) {
}

package com.empresax.sistema.relatorios.caixa;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Um caixa aberto no período, como o relatório o enxerga: valores do fechamento (nulos enquanto
 * aberto) e os totais de reposição e sangria. Nomes já resolvidos (a sessão guarda e-mails).
 */
public record CaixaDoPeriodo(
        UUID id,
        String operador,
        String operadorNome,
        String abertaPorNome,
        String fechadaPorNome,
        Instant abertaEm,
        Instant fechadaEm,
        BigDecimal fundoInicial,
        BigDecimal reposicoes,
        BigDecimal sangrias,
        BigDecimal vendasEmDinheiro,
        BigDecimal valorEsperado,
        BigDecimal valorContado,
        String observacao
) {

    public boolean fechado() {
        return fechadaEm != null;
    }

    /** Sobra (positivo) ou falta (negativo); vazio enquanto o caixa está aberto. */
    public Optional<BigDecimal> diferenca() {
        return fechado() ? Optional.of(valorContado.subtract(valorEsperado)) : Optional.empty();
    }

    public ResultadoFechamento resultado() {
        return diferenca().map(ResultadoFechamento::de).orElse(ResultadoFechamento.ABERTO);
    }
}

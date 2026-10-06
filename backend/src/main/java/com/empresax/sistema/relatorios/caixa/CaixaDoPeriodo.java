package com.empresax.sistema.relatorios.caixa;

import com.empresax.sistema.relatorios.vendas.PeriodoRelatorio;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Um caixa aberto no período, como o relatório o enxerga: qual caixa físico, quem operou, valores do
 * fechamento (nulos enquanto aberto), reposições, sangrias e o vendido em cada forma de pagamento.
 * Nomes já resolvidos (a sessão guarda e-mails).
 */
public record CaixaDoPeriodo(
        UUID id,
        String pontoNome,
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
        String observacao,
        /** Relatório da maquininha − sistema, somado nas formas conferidas no fechamento (D40). Nulo = não conferido. */
        BigDecimal diferencaMaquininha,
        /** Forma (DINHEIRO, CARTAO_CREDITO, CARTAO_DEBITO, PIX, PIX_QR) → valor vendido. */
        Map<String, BigDecimal> vendasPorForma
) {

    public CaixaDoPeriodo {
        vendasPorForma = Map.copyOf(vendasPorForma);
    }

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

    /**
     * O que entrou na gaveta por venda: contado − valor inicial − reposições + sangrias. Tem que
     * dar o vendido em dinheiro (é a "autenticação" do dinheiro). Vazio enquanto aberto.
     */
    public Optional<BigDecimal> dinheiroQueEntrou() {
        return fechado()
                ? Optional.of(valorContado.subtract(fundoInicial).subtract(reposicoes).add(sangrias))
                : Optional.empty();
    }

    public BigDecimal totalVendido() {
        return vendasPorForma.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Dia do caixa = dia da abertura, no fuso da loja. */
    public LocalDate dia() {
        return abertaEm.atZone(PeriodoRelatorio.FUSO_DA_LOJA).toLocalDate();
    }
}

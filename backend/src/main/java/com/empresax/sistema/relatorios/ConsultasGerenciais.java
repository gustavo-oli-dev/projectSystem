package com.empresax.sistema.relatorios;

import com.empresax.sistema.cobranca.Cobranca;
import com.empresax.sistema.cobranca.CobrancaRepository;
import com.empresax.sistema.cobranca.StatusCobranca;
import com.empresax.sistema.pedido.PedidoRepository;
import com.empresax.sistema.pedido.StatusPedido;
import com.empresax.sistema.shared.dinheiro.Dinheiro;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Consultas somente-leitura sobre o estado geral do negócio. Usadas por mais de um consumidor
 * (painel administrativo, assistente de IA do gestor — RF02/D6 em DECISOES.md), daí viver num
 * pacote neutro em vez de dentro de um dos dois.
 */
@Service
public class ConsultasGerenciais {

    private final CobrancaRepository cobrancaRepository;
    private final PedidoRepository pedidoRepository;

    public ConsultasGerenciais(CobrancaRepository cobrancaRepository, PedidoRepository pedidoRepository) {
        this.cobrancaRepository = cobrancaRepository;
        this.pedidoRepository = pedidoRepository;
    }

    @Transactional(readOnly = true)
    public ResultadoFaturamento consultarFaturamento(LocalDate dataInicio, LocalDate dataFim) {
        Instant inicio = dataInicio.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant fim = dataFim.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        List<Cobranca> cobrancasPagas =
                cobrancaRepository.findByStatusAndCriadoEmBetween(StatusCobranca.PAGA, inicio, fim);

        Dinheiro total = cobrancasPagas.stream()
                .map(Cobranca::valor)
                .reduce(Dinheiro.zero(), Dinheiro::somar);

        return new ResultadoFaturamento(total.valor(), cobrancasPagas.size());
    }

    @Transactional(readOnly = true)
    public Map<StatusPedido, Long> consultarPedidosPorStatus() {
        Map<StatusPedido, Long> contagem = new EnumMap<>(StatusPedido.class);
        for (StatusPedido status : StatusPedido.values()) {
            contagem.put(status, pedidoRepository.countByStatus(status));
        }
        return contagem;
    }

    @Transactional(readOnly = true)
    public List<ResumoCobranca> consultarCobrancasPendentes() {
        return cobrancaRepository.findByStatus(StatusCobranca.PENDENTE).stream()
                .map(cobranca -> new ResumoCobranca(
                        cobranca.pedidoId(), cobranca.meio().name(), cobranca.valor().valor(), cobranca.criadoEm()))
                .toList();
    }
}

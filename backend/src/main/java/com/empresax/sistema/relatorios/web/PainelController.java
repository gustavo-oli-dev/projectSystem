package com.empresax.sistema.relatorios.web;

import com.empresax.sistema.acesso.RegraAcesso;
import com.empresax.sistema.pedido.StatusPedido;
import com.empresax.sistema.relatorios.ConsultasGerenciais;
import com.empresax.sistema.relatorios.ResultadoFaturamento;
import com.empresax.sistema.relatorios.ResumoCobranca;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Números para os widgets do painel administrativo. Mesma fonte de verdade do assistente de IA
 * do gestor (ConsultasGerenciais) — nenhum cálculo duplicado entre os dois. Cada widget exige a
 * permissão da área que mostra: um cargo pode ver o painel sem ver o faturamento.
 */
@RestController
@RequestMapping("/api/painel")
public class PainelController {

    private final ConsultasGerenciais consultas;

    public PainelController(ConsultasGerenciais consultas) {
        this.consultas = consultas;
    }

    @PreAuthorize(RegraAcesso.FATURAMENTO_VER)
    @GetMapping("/faturamento")
    public ResultadoFaturamento faturamento(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim
    ) {
        return consultas.consultarFaturamento(dataInicio, dataFim);
    }

    @PreAuthorize(RegraAcesso.PEDIDOS_VER)
    @GetMapping("/pedidos-por-status")
    public Map<StatusPedido, Long> pedidosPorStatus() {
        return consultas.consultarPedidosPorStatus();
    }

    @PreAuthorize(RegraAcesso.COBRANCAS_VER)
    @GetMapping("/cobrancas-pendentes")
    public List<ResumoCobranca> cobrancasPendentes() {
        return consultas.consultarCobrancasPendentes();
    }
}

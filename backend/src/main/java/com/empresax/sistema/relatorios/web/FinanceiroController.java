package com.empresax.sistema.relatorios.web;

import com.empresax.sistema.acesso.RegraAcesso;
import com.empresax.sistema.relatorios.financeiro.ExportacaoFinanceiroCsv;
import com.empresax.sistema.relatorios.financeiro.FluxoDeCaixa;
import com.empresax.sistema.relatorios.financeiro.FluxoDeCaixaService;
import com.empresax.sistema.relatorios.vendas.PeriodoRelatorio;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** Área do Financeiro (D40): fluxo de caixa do período e exportação dos lançamentos. */
@RestController
@RequestMapping("/api/financeiro")
@PreAuthorize(RegraAcesso.FINANCEIRO_VER)
public class FinanceiroController {

    private static final MediaType CSV = new MediaType("text", "csv", StandardCharsets.UTF_8);

    private final FluxoDeCaixaService fluxoDeCaixaService;

    public FinanceiroController(FluxoDeCaixaService fluxoDeCaixaService) {
        this.fluxoDeCaixaService = fluxoDeCaixaService;
    }

    public record DiaResponse(LocalDate dia, BigDecimal entradas, BigDecimal saidas, BigDecimal saldo) {
    }

    public record FluxoDeCaixaResponse(
            BigDecimal totalEntradas,
            BigDecimal totalSaidas,
            BigDecimal saldo,
            BigDecimal aPagarNoPeriodo,
            Map<String, BigDecimal> entradasPorForma,
            List<DiaResponse> porDia,
            List<FluxoDeCaixa.Saida> saidas
    ) {

        static FluxoDeCaixaResponse de(FluxoDeCaixa fluxo) {
            return new FluxoDeCaixaResponse(
                    fluxo.totalEntradas(), fluxo.totalSaidas(), fluxo.saldo(), fluxo.aPagarNoPeriodo(), fluxo.entradasPorForma(),
                    fluxo.porDia().stream().map(dia -> new DiaResponse(dia.dia(), dia.entradas(), dia.saidas(), dia.saldo())).toList(),
                    fluxo.saidas());
        }
    }

    @GetMapping("/fluxo-de-caixa")
    public FluxoDeCaixaResponse fluxoDeCaixa(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim
    ) {
        return FluxoDeCaixaResponse.de(fluxoDeCaixaService.gerar(new PeriodoRelatorio(inicio, fim)));
    }

    @GetMapping("/lancamentos.csv")
    public ResponseEntity<byte[]> exportar(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim
    ) {
        PeriodoRelatorio periodo = new PeriodoRelatorio(inicio, fim);
        byte[] arquivo = ExportacaoFinanceiroCsv.lancamentos(fluxoDeCaixaService.gerar(periodo));
        String nomeArquivo = "financeiro_" + periodo.inicio() + "_a_" + periodo.fim() + ".csv";
        return ResponseEntity.ok()
                .contentType(CSV)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(nomeArquivo).build().toString())
                .body(arquivo);
    }
}

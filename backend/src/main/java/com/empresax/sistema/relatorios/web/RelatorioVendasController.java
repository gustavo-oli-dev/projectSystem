package com.empresax.sistema.relatorios.web;

import com.empresax.sistema.acesso.RegraAcesso;
import com.empresax.sistema.relatorios.vendas.ExportacaoCsv;
import com.empresax.sistema.relatorios.vendas.PeriodoRelatorio;
import com.empresax.sistema.relatorios.vendas.RelatorioVendas;
import com.empresax.sistema.relatorios.vendas.RelatorioVendasService;
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

import java.time.LocalDate;
import java.util.function.Function;

/** Relatórios do painel. Valores em dinheiro (faturamento, lucro) exigem ver faturamento. */
@RestController
@RequestMapping("/api/relatorios/vendas")
@PreAuthorize(RegraAcesso.PAINEL_VENDAS + " or " + RegraAcesso.PAINEL_PRODUTOS + " or " + RegraAcesso.PAINEL_HORARIOS)
public class RelatorioVendasController {

    private static final MediaType CSV = new MediaType("text", "csv", java.nio.charset.StandardCharsets.UTF_8);

    private final RelatorioVendasService relatorioVendasService;

    public RelatorioVendasController(RelatorioVendasService relatorioVendasService) {
        this.relatorioVendasService = relatorioVendasService;
    }

    @GetMapping
    public RelatorioVendasResponse gerar(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim
    ) {
        return RelatorioVendasResponse.de(relatorioVendasService.gerar(new PeriodoRelatorio(inicio, fim)));
    }

    /** CSV para Excel: vendas por dia/semana/mês do período. */
    @GetMapping("/periodos.csv")
    public ResponseEntity<byte[]> exportarPeriodos(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim
    ) {
        return exportar(new PeriodoRelatorio(inicio, fim), "vendas-por-periodo", ExportacaoCsv::vendasPorPeriodo);
    }

    /** CSV para Excel: itens mais vendidos do período. */
    @GetMapping("/mais-vendidos.csv")
    public ResponseEntity<byte[]> exportarMaisVendidos(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim
    ) {
        return exportar(new PeriodoRelatorio(inicio, fim), "mais-vendidos", ExportacaoCsv::maisVendidos);
    }

    private ResponseEntity<byte[]> exportar(
            PeriodoRelatorio periodo, String nome, Function<RelatorioVendas, byte[]> gerarCsv
    ) {
        byte[] arquivo = gerarCsv.apply(relatorioVendasService.gerar(periodo));
        String nomeArquivo = nome + "_" + periodo.inicio() + "_a_" + periodo.fim() + ".csv";
        return ResponseEntity.ok()
                .contentType(CSV)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(nomeArquivo).build().toString())
                .body(arquivo);
    }
}

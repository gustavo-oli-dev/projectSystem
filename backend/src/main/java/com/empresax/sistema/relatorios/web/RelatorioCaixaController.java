package com.empresax.sistema.relatorios.web;

import com.empresax.sistema.acesso.RegraAcesso;
import com.empresax.sistema.relatorios.caixa.ExportacaoCaixaCsv;
import com.empresax.sistema.relatorios.caixa.RelatorioCaixaService;
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

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

/** Relatório da gestão de caixa no painel: quem vê faturamento ou confere caixas. */
@RestController
@RequestMapping("/api/relatorios/caixa")
@PreAuthorize(RegraAcesso.FATURAMENTO_VER + " or " + RegraAcesso.CAIXA_CONFERIR)
public class RelatorioCaixaController {

    private static final MediaType CSV = new MediaType("text", "csv", StandardCharsets.UTF_8);

    private final RelatorioCaixaService relatorioCaixaService;

    public RelatorioCaixaController(RelatorioCaixaService relatorioCaixaService) {
        this.relatorioCaixaService = relatorioCaixaService;
    }

    @GetMapping
    public RelatorioCaixaResponse gerar(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim
    ) {
        return RelatorioCaixaResponse.de(relatorioCaixaService.gerar(new PeriodoRelatorio(inicio, fim)));
    }

    /** Conferência do dinheiro de um dia (o gerente compara a gaveta com o que o sistema registrou). */
    @GetMapping("/dia")
    public DinheiroDoDiaResponse conferirDia(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
        return DinheiroDoDiaResponse.de(relatorioCaixaService.conferirDia(data));
    }

    @GetMapping("/fechamentos.csv")
    public ResponseEntity<byte[]> exportarFechamentos(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim
    ) {
        PeriodoRelatorio periodo = new PeriodoRelatorio(inicio, fim);
        byte[] arquivo = ExportacaoCaixaCsv.fechamentos(relatorioCaixaService.gerar(periodo));
        String nomeArquivo = "fechamentos-de-caixa_" + periodo.inicio() + "_a_" + periodo.fim() + ".csv";
        return ResponseEntity.ok()
                .contentType(CSV)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(nomeArquivo).build().toString())
                .body(arquivo);
    }
}

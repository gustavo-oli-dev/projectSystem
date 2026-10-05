package com.empresax.sistema.relatorios.web;

import com.empresax.sistema.acesso.RegraAcesso;
import com.empresax.sistema.relatorios.estoque.RelatorioPerdas;
import com.empresax.sistema.relatorios.estoque.RelatorioPerdasService;
import com.empresax.sistema.relatorios.vendas.PeriodoRelatorio;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Perdas e quebras no painel (valores em dinheiro: exige ver faturamento). */
@RestController
@RequestMapping("/api/relatorios/perdas")
@PreAuthorize(RegraAcesso.FATURAMENTO_VER)
public class RelatorioPerdasController {

    private final RelatorioPerdasService relatorioPerdasService;

    public RelatorioPerdasController(RelatorioPerdasService relatorioPerdasService) {
        this.relatorioPerdasService = relatorioPerdasService;
    }

    @GetMapping
    public RelatorioPerdasResponse gerar(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim
    ) {
        return RelatorioPerdasResponse.de(relatorioPerdasService.gerar(new PeriodoRelatorio(inicio, fim)));
    }

    public record RelatorioPerdasResponse(
            int unidades, BigDecimal valor, int unidadesSemCusto,
            List<ItemPerda> porMotivo, List<ItemPerda> porProduto
    ) {

        public record ItemPerda(String chave, int unidades, BigDecimal valor) {
        }

        static RelatorioPerdasResponse de(RelatorioPerdas relatorio) {
            return new RelatorioPerdasResponse(
                    relatorio.unidades(), relatorio.valor(), relatorio.unidadesSemCusto(),
                    relatorio.porMotivo().stream().map(item -> new ItemPerda(item.motivo(), item.unidades(), item.valor())).toList(),
                    relatorio.porProduto().stream().map(item -> new ItemPerda(item.produto(), item.unidades(), item.valor())).toList());
        }
    }
}

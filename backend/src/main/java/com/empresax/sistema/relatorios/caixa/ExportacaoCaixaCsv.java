package com.empresax.sistema.relatorios.caixa;

import com.empresax.sistema.relatorios.vendas.ExportacaoCsv;
import com.empresax.sistema.relatorios.vendas.PeriodoRelatorio;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Stream;

/** CSV para Excel com um caixa por linha (mesmo formato dos CSVs de vendas). */
public final class ExportacaoCaixaCsv {

    private static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
            .withZone(PeriodoRelatorio.FUSO_DA_LOJA);
    private static final List<String> CABECALHO = List.of(
            "Operador", "Aberto em", "Aberto por", "Fechado em", "Fechado por", "Fundo de troco (R$)",
            "Vendas em dinheiro (R$)", "Reposições (R$)", "Sangrias (R$)", "Esperado (R$)", "Contado (R$)",
            "Diferença (R$)", "Resultado", "Observação");

    private ExportacaoCaixaCsv() {
    }

    public static byte[] fechamentos(RelatorioCaixa relatorio) {
        Stream<List<String>> linhas = relatorio.caixas().stream().map(caixa -> List.of(
                ExportacaoCsv.texto(caixa.operadorNome()),
                dataHora(caixa.abertaEm()),
                ExportacaoCsv.texto(caixa.abertaPorNome()),
                dataHora(caixa.fechadaEm()),
                ExportacaoCsv.texto(caixa.fechadaPorNome()),
                decimal(caixa.fundoInicial()),
                decimal(caixa.vendasEmDinheiro()),
                decimal(caixa.reposicoes()),
                decimal(caixa.sangrias()),
                decimal(caixa.valorEsperado()),
                decimal(caixa.valorContado()),
                caixa.diferenca().map(ExportacaoCsv::decimal).orElse(""),
                rotulo(caixa.resultado()),
                ExportacaoCsv.texto(caixa.observacao())));
        return ExportacaoCsv.montar(CABECALHO, linhas);
    }

    private static String rotulo(ResultadoFechamento resultado) {
        return switch (resultado) {
            case ABERTO -> "Ainda aberto";
            case BATEU -> "Bateu";
            case SOBROU -> "Sobrou";
            case FALTOU -> "Faltou";
        };
    }

    private static String dataHora(Instant instante) {
        return instante == null ? "" : DATA_HORA.format(instante);
    }

    private static String decimal(BigDecimal valor) {
        return valor == null ? "" : ExportacaoCsv.decimal(valor);
    }
}

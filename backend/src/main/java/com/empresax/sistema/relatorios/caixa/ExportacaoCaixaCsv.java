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
            "Caixa", "Operador", "Aberto em", "Aberto por", "Fechado em", "Fechado por", "Fundo de troco (R$)",
            "Vendas em dinheiro (R$)", "Reposições (R$)", "Sangrias (R$)", "Esperado (R$)", "Contado (R$)",
            "Diferença (R$)", "Resultado", "Diferença da maquininha (R$)", "Crédito (R$)", "Débito (R$)",
            "Pix na maquininha (R$)", "Pix por QR (R$)", "Total vendido (R$)", "Observação");

    private ExportacaoCaixaCsv() {
    }

    public static byte[] fechamentos(RelatorioCaixa relatorio) {
        Stream<List<String>> linhas = relatorio.caixas().stream().map(caixa -> List.of(
                ExportacaoCsv.texto(caixa.pontoNome()),
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
                decimal(caixa.diferencaMaquininha()),
                forma(caixa, "CARTAO_CREDITO"),
                forma(caixa, "CARTAO_DEBITO"),
                forma(caixa, "PIX"),
                forma(caixa, "PIX_QR"),
                ExportacaoCsv.decimal(caixa.totalVendido()),
                ExportacaoCsv.texto(caixa.observacao())));
        return ExportacaoCsv.montar(CABECALHO, linhas);
    }

    private static String rotulo(ResultadoFechamento resultado) {
        return switch (resultado) {
            case ABERTO -> "Ainda aberto";
            case BATEU -> "Certo";
            case SOBROU -> "Sobrando";
            case FALTOU -> "Devendo";
        };
    }

    private static String forma(CaixaDoPeriodo caixa, String forma) {
        return ExportacaoCsv.decimal(caixa.vendasPorForma().getOrDefault(forma, BigDecimal.ZERO));
    }

    private static String dataHora(Instant instante) {
        return instante == null ? "" : DATA_HORA.format(instante);
    }

    private static String decimal(BigDecimal valor) {
        return valor == null ? "" : ExportacaoCsv.decimal(valor);
    }
}

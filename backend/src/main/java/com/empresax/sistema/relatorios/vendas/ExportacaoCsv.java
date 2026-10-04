package com.empresax.sistema.relatorios.vendas;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * CSV no formato que o Excel em português abre direto: separador ";", vírgula decimal, UTF-8 com
 * BOM (acentos certos). Texto que começa com = + - @ ganha um apóstrofo na frente, para o Excel
 * não executá-lo como fórmula (injeção de fórmula em CSV).
 */
public final class ExportacaoCsv {

    private static final String SEPARADOR = ";";
    private static final String QUEBRA = "\r\n";
    private static final byte[] BOM_UTF8 = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
    private static final String INICIOS_DE_FORMULA = "=+-@\t\r";

    private ExportacaoCsv() {
    }

    public static byte[] vendasPorPeriodo(RelatorioVendas relatorio) {
        Stream<List<String>> linhas = relatorio.serie().stream().map(ponto -> List.of(
                ponto.inicio().toString(),
                String.valueOf(ponto.totais().vendas()),
                String.valueOf(ponto.totais().unidades()),
                decimal(ponto.totais().faturamento()),
                decimal(ponto.totais().lucroBruto())));
        return montar(List.of("Início do período", "Vendas", "Unidades", "Faturamento (R$)", "Lucro bruto (R$)"), linhas);
    }

    public static byte[] maisVendidos(RelatorioVendas relatorio) {
        Stream<List<String>> linhas = relatorio.maisVendidos().stream().map(item -> List.of(
                texto(item.descricao()),
                "PRODUTO".equals(item.tipo()) ? "Produto" : "Serviço",
                String.valueOf(item.unidades()),
                decimal(item.faturamento()),
                item.lucroBruto() == null ? "custo não informado" : decimal(item.lucroBruto()),
                item.custoCompleto() ? "completo" : "parcial (há vendas sem custo)"));
        return montar(List.of("Item", "Tipo", "Unidades", "Faturamento (R$)", "Lucro bruto (R$)", "Lucro"), linhas);
    }

    public static byte[] montar(List<String> cabecalho, Stream<List<String>> linhas) {
        String corpo = Stream.concat(Stream.of(cabecalho), linhas)
                .map(colunas -> colunas.stream().map(ExportacaoCsv::campo).collect(Collectors.joining(SEPARADOR)))
                .collect(Collectors.joining(QUEBRA, "", QUEBRA));
        byte[] conteudo = corpo.getBytes(StandardCharsets.UTF_8);
        byte[] arquivo = new byte[BOM_UTF8.length + conteudo.length];
        System.arraycopy(BOM_UTF8, 0, arquivo, 0, BOM_UTF8.length);
        System.arraycopy(conteudo, 0, arquivo, BOM_UTF8.length, conteudo.length);
        return arquivo;
    }

    /** Aspas em volta quando o campo tem separador, aspas ou quebra de linha. */
    private static String campo(String valor) {
        boolean precisaAspas = valor.contains(SEPARADOR) || valor.contains("\"") || valor.contains("\n");
        return precisaAspas ? "\"" + valor.replace("\"", "\"\"") + "\"" : valor;
    }

    public static String texto(String valor) {
        if (valor == null || valor.isEmpty()) {
            return "";
        }
        return INICIOS_DE_FORMULA.indexOf(valor.charAt(0)) >= 0 ? "'" + valor : valor;
    }

    public static String decimal(BigDecimal valor) {
        return valor.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString().replace('.', ',');
    }
}

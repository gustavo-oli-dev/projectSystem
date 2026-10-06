package com.empresax.sistema.relatorios.financeiro;

import com.empresax.sistema.relatorios.vendas.ExportacaoCsv;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/** Lançamentos do fluxo de caixa para abrir no Excel (D40): uma linha por entrada e por saída. */
public final class ExportacaoFinanceiroCsv {

    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final List<String> CABECALHO = List.of("Data", "Tipo", "Descrição", "Contato", "Valor (R$)");
    private static final String ENTRADA = "Entrada";
    private static final String SAIDA = "Saída";
    private static final Map<String, String> ROTULO_FORMA = Map.of(
            "DINHEIRO", "Vendas em dinheiro",
            "CARTAO_CREDITO", "Vendas no crédito",
            "CARTAO_DEBITO", "Vendas no débito",
            "PIX", "Vendas no Pix (maquininha)",
            "PIX_ONLINE", "Pix (QR ou online)",
            "BOLETO_ONLINE", "Boleto");

    private ExportacaoFinanceiroCsv() {
    }

    public static byte[] lancamentos(FluxoDeCaixa fluxo) {
        Stream<List<String>> entradas = fluxo.entradas().stream().map(entrada -> List.of(
                DATA.format(entrada.dia()), ENTRADA, ROTULO_FORMA.getOrDefault(entrada.forma(), entrada.forma()), "",
                ExportacaoCsv.decimal(entrada.valor())));
        Stream<List<String>> saidas = fluxo.saidas().stream().map(saida -> List.of(
                DATA.format(saida.dia()), SAIDA, ExportacaoCsv.texto(saida.descricao()), ExportacaoCsv.texto(saida.contato()),
                "-" + ExportacaoCsv.decimal(saida.valor())));
        return ExportacaoCsv.montar(CABECALHO, Stream.concat(entradas, saidas));
    }
}

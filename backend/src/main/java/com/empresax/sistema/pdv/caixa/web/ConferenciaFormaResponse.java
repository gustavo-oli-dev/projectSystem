package com.empresax.sistema.pdv.caixa.web;

import com.empresax.sistema.pdv.FormaPagamentoPresencial;
import com.empresax.sistema.pdv.caixa.SessaoCaixa;

import java.math.BigDecimal;
import java.util.List;

/** Uma forma da maquininha no fechamento: sistema × relatório da maquininha. */
public record ConferenciaFormaResponse(FormaPagamentoPresencial forma, BigDecimal valorSistema, BigDecimal valorInformado, BigDecimal diferenca) {

    static List<ConferenciaFormaResponse> de(SessaoCaixa sessao) {
        return sessao.conferenciasForma().stream()
                .map(conferencia -> new ConferenciaFormaResponse(
                        conferencia.forma(),
                        conferencia.valorSistema().valor(),
                        conferencia.valorInformado().valor(),
                        conferencia.diferenca()))
                .toList();
    }
}

package com.empresax.sistema.pdv.caixa.web;

import com.empresax.sistema.pdv.caixa.MovimentoCaixa;
import com.empresax.sistema.pdv.caixa.SessaoCaixa;
import com.empresax.sistema.pdv.caixa.TipoMovimentoCaixa;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record MovimentoCaixaResponse(
        UUID id,
        TipoMovimentoCaixa tipo,
        BigDecimal valor,
        String motivo,
        List<CedulaContadaResponse> cedulas,
        String registradoPor,
        Instant registradoEm
) {

    static MovimentoCaixaResponse de(MovimentoCaixa movimento) {
        return new MovimentoCaixaResponse(
                movimento.id(),
                movimento.tipo(),
                movimento.valor().valor(),
                movimento.motivo(),
                CedulaContadaResponse.de(movimento.cedulas()),
                movimento.registradoPor(),
                movimento.registradoEm());
    }

    static List<MovimentoCaixaResponse> de(SessaoCaixa sessao) {
        return sessao.movimentos().stream().map(MovimentoCaixaResponse::de).toList();
    }
}

package com.empresax.sistema.pdv.caixa.web;

import com.empresax.sistema.pdv.caixa.SessaoCaixa;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * O caixa aberto, visto pelo próprio operador. De propósito não traz o valor esperado na gaveta:
 * o fechamento é cego (quem conta não sabe quanto "deveria" dar).
 */
public record CaixaAbertoResponse(
        UUID id,
        Instant abertaEm,
        BigDecimal fundoInicial,
        List<CedulaContadaResponse> cedulasAbertura,
        BigDecimal totalSuprimentos,
        BigDecimal totalSangrias,
        List<MovimentoCaixaResponse> movimentos
) {

    static CaixaAbertoResponse de(SessaoCaixa sessao) {
        return new CaixaAbertoResponse(
                sessao.id(),
                sessao.abertaEm(),
                sessao.fundoInicial().valor(),
                CedulaContadaResponse.de(sessao.cedulasAbertura()),
                sessao.totalSuprimentos().valor(),
                sessao.totalSangrias().valor(),
                MovimentoCaixaResponse.de(sessao));
    }
}

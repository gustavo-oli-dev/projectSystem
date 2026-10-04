package com.empresax.sistema.pdv.caixa.web;

import com.empresax.sistema.pdv.caixa.SessaoCaixa;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Um caixa aberto, para a tela de venda e a gestão de caixa. De propósito não traz o valor esperado
 * na gaveta: o fechamento é cego (quem conta não sabe quanto "deveria" dar).
 */
public record CaixaAbertoResponse(
        UUID id,
        String operador,
        String operadorNome,
        String abertaPorNome,
        Instant abertaEm,
        BigDecimal fundoInicial,
        List<CedulaContadaResponse> cedulasAbertura,
        BigDecimal totalSuprimentos,
        BigDecimal totalSangrias,
        List<MovimentoCaixaResponse> movimentos
) {

    static CaixaAbertoResponse de(SessaoCaixa sessao, Map<String, String> nomes) {
        return new CaixaAbertoResponse(
                sessao.id(),
                sessao.operador(),
                nomes.getOrDefault(sessao.operador(), sessao.operador()),
                nomes.getOrDefault(sessao.abertaPor(), sessao.abertaPor()),
                sessao.abertaEm(),
                sessao.fundoInicial().valor(),
                CedulaContadaResponse.de(sessao.cedulasAbertura()),
                sessao.totalSuprimentos().valor(),
                sessao.totalSangrias().valor(),
                MovimentoCaixaResponse.de(sessao));
    }
}

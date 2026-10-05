package com.empresax.sistema.pdv.caixa.web;

import com.empresax.sistema.pdv.caixa.CaixaService.ResumoSessaoCaixa;
import com.empresax.sistema.pdv.caixa.SessaoCaixa;
import com.empresax.sistema.pdv.caixa.StatusSessaoCaixa;
import com.empresax.sistema.shared.dinheiro.Dinheiro;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Linha da lista de conferência de caixas. Caixa aberto ainda não tem contagem nem diferença. */
public record ResumoCaixaResponse(
        UUID id,
        String pontoNome,
        String operador,
        String operadorNome,
        StatusSessaoCaixa status,
        Instant abertaEm,
        Instant fechadaEm,
        BigDecimal fundoInicial,
        BigDecimal vendasEmDinheiro,
        BigDecimal valorEsperado,
        BigDecimal valorContado,
        BigDecimal diferenca
) {

    static ResumoCaixaResponse de(ResumoSessaoCaixa resumo, NomesDoCaixa nomes) {
        SessaoCaixa sessao = resumo.sessao();
        Dinheiro vendasEmDinheiro = sessao.vendasEmDinheiro().orElse(resumo.vendasEmDinheiroAteAgora());
        return new ResumoCaixaResponse(
                sessao.id(),
                nomes.caixa(sessao.pontoCaixaId()),
                sessao.operador(),
                nomes.pessoa(sessao.operador()),
                sessao.status(),
                sessao.abertaEm(),
                sessao.fechadaEm().orElse(null),
                sessao.fundoInicial().valor(),
                vendasEmDinheiro.valor(),
                sessao.valorEsperado().orElseGet(() -> sessao.dinheiroEsperado(vendasEmDinheiro)),
                sessao.valorContado().map(Dinheiro::valor).orElse(null),
                sessao.diferenca().orElse(null));
    }
}

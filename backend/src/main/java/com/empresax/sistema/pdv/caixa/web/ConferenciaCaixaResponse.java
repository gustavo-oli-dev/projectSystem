package com.empresax.sistema.pdv.caixa.web;

import com.empresax.sistema.pdv.caixa.CaixaService.FechamentoCaixa;
import com.empresax.sistema.pdv.caixa.SessaoCaixa;
import com.empresax.sistema.pdv.caixa.StatusSessaoCaixa;
import com.empresax.sistema.pdv.caixa.VendasDoCaixaConsulta.VendasPorForma;
import com.empresax.sistema.shared.dinheiro.Dinheiro;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Conferência completa de um caixa: abertura, movimentos, vendas por forma de pagamento e, se já
 * fechado, contagem, esperado e diferença. Com o caixa ainda aberto, o esperado é o de agora.
 */
public record ConferenciaCaixaResponse(
        UUID id,
        String operador,
        String operadorNome,
        StatusSessaoCaixa status,
        Instant abertaEm,
        Instant fechadaEm,
        BigDecimal fundoInicial,
        List<CedulaContadaResponse> cedulasAbertura,
        List<MovimentoCaixaResponse> movimentos,
        BigDecimal totalSuprimentos,
        BigDecimal totalSangrias,
        List<VendaPorFormaResponse> vendasPorForma,
        BigDecimal vendasEmDinheiro,
        BigDecimal valorEsperado,
        BigDecimal valorContado,
        List<CedulaContadaResponse> cedulasFechamento,
        BigDecimal diferenca,
        BigDecimal dinheiroQueEntrou,
        String observacao
) {

    private static final String FORMA_DINHEIRO = "DINHEIRO";

    public record VendaPorFormaResponse(String forma, int vendas, BigDecimal valor) {
    }

    static ConferenciaCaixaResponse de(FechamentoCaixa fechamento, Map<String, String> nomes) {
        SessaoCaixa sessao = fechamento.sessao();
        Dinheiro vendasEmDinheiro = sessao.vendasEmDinheiro().orElseGet(() -> fechamento.vendasPorForma().stream()
                .filter(forma -> FORMA_DINHEIRO.equals(forma.forma()))
                .map(VendasPorForma::valor)
                .findFirst()
                .orElse(Dinheiro.zero()));
        return new ConferenciaCaixaResponse(
                sessao.id(),
                sessao.operador(),
                nomes.getOrDefault(sessao.operador(), sessao.operador()),
                sessao.status(),
                sessao.abertaEm(),
                sessao.fechadaEm().orElse(null),
                sessao.fundoInicial().valor(),
                CedulaContadaResponse.de(sessao.cedulasAbertura()),
                MovimentoCaixaResponse.de(sessao),
                sessao.totalSuprimentos().valor(),
                sessao.totalSangrias().valor(),
                fechamento.vendasPorForma().stream()
                        .map(forma -> new VendaPorFormaResponse(forma.forma(), forma.vendas(), forma.valor().valor()))
                        .toList(),
                vendasEmDinheiro.valor(),
                sessao.valorEsperado().orElseGet(() -> sessao.dinheiroEsperado(vendasEmDinheiro)),
                sessao.valorContado().map(Dinheiro::valor).orElse(null),
                CedulaContadaResponse.de(sessao.cedulasFechamento()),
                sessao.diferenca().orElse(null),
                sessao.dinheiroQueEntrou().orElse(null),
                sessao.observacaoFechamento().orElse(null));
    }
}

package com.empresax.sistema.relatorios.caixa;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Conferência do dinheiro de um dia (D31), feita pelo gerente. O sistema sabe o valor dos produtos
 * vendidos em dinheiro; o dinheiro contado nas gavetas, menos o valor inicial (e reposições/sangrias),
 * tem que dar exatamente esse valor — é a "autenticação" do dinheiro. Só caixas fechados entram na
 * comparação; os ainda abertos são contados à parte (o dia só fecha quando todos fecharem).
 */
public record DinheiroDoDia(
        LocalDate dia,
        List<CaixaDoPeriodo> caixas,
        List<ProdutoEmDinheiro> produtosEmDinheiro
) {

    public DinheiroDoDia {
        caixas = List.copyOf(caixas);
        produtosEmDinheiro = List.copyOf(produtosEmDinheiro);
    }

    public record ProdutoEmDinheiro(String descricao, int quantidade, BigDecimal valor) {
    }

    /** Vendido em dinheiro nos caixas já fechados (valor congelado no fechamento). */
    public BigDecimal vendidoEmDinheiro() {
        return fechados().stream().map(CaixaDoPeriodo::vendasEmDinheiro).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Somado de todas as gavetas fechadas: contado − valor inicial − reposições + sangrias. */
    public BigDecimal entrouNasGavetas() {
        return fechados().stream().flatMap(caixa -> caixa.dinheiroQueEntrou().stream()).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Entrou − vendido: zero = certo; negativo = devendo; positivo = sobrando. */
    public BigDecimal diferenca() {
        return entrouNasGavetas().subtract(vendidoEmDinheiro());
    }

    public int caixasAbertos() {
        return caixas.size() - fechados().size();
    }

    /** Sem nenhum caixa fechado não há o que comparar. */
    public Optional<ResultadoFechamento> resultado() {
        return fechados().isEmpty() ? Optional.empty() : Optional.of(ResultadoFechamento.de(diferenca()));
    }

    private List<CaixaDoPeriodo> fechados() {
        return caixas.stream().filter(CaixaDoPeriodo::fechado).toList();
    }
}

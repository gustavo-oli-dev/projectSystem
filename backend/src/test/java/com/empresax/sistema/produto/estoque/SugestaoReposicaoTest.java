package com.empresax.sistema.produto.estoque;

import com.empresax.sistema.common.domain.DomainException;
import com.empresax.sistema.produto.Produto;
import com.empresax.sistema.shared.dinheiro.Dinheiro;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SugestaoReposicaoTest {

    @Test
    void sugereCobrir30DiasDeVendaEManterOMinimo() {
        Produto arroz = produto(4, 10);

        // Vendeu 60 em 30 dias; mínimo 10; tem 4 → comprar 60 + 10 − 4 = 66.
        assertThat(SugestaoReposicao.para(arroz, 60).quantidadeSugerida()).isEqualTo(66);
    }

    @Test
    void semVendasSugereCompletarOMinimo() {
        assertThat(SugestaoReposicao.para(produto(2, 10), 0).quantidadeSugerida()).isEqualTo(8);
    }

    @Test
    void nuncaSugereMenosDeUmaUnidade() {
        assertThat(SugestaoReposicao.para(produto(10, 10), 0).quantidadeSugerida()).isEqualTo(1);
    }

    @Test
    void precisaReporQuandoChegaNoMinimoEUsaOPadraoSemMinimoDefinido() {
        Produto comMinimo = produto(10, 10);
        Produto semMinimo = produto(Produto.ESTOQUE_MINIMO_PADRAO + 1, null);

        assertThat(comMinimo.precisaRepor()).isTrue();
        assertThat(semMinimo.precisaRepor()).isFalse();
        assertThat(semMinimo.estoqueMinimoEfetivo()).isEqualTo(Produto.ESTOQUE_MINIMO_PADRAO);
    }

    @Test
    void estoqueMinimoNegativoEhRecusado() {
        assertThatThrownBy(() -> produto(1, -1)).isInstanceOf(DomainException.class);
    }

    private static Produto produto(int estoque, Integer minimo) {
        Produto produto = new Produto("Arroz 5kg", null, "10063021", "un", new Dinheiro(new BigDecimal("25.90")));
        produto.darEntradaNoEstoque(estoque);
        produto.definirEstoqueMinimo(minimo);
        return produto;
    }
}

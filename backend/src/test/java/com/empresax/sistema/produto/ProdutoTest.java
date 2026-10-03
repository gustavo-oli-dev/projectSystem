package com.empresax.sistema.produto;

import com.empresax.sistema.common.domain.DomainException;
import com.empresax.sistema.shared.dinheiro.Dinheiro;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProdutoTest {

    private static final Dinheiro PRECO = new Dinheiro(new BigDecimal("99.90"));

    @Test
    void cadastraProdutoAtivoPorPadrao() {
        Produto produto = new Produto("Caneca", "Caneca de porcelana", "69120000", "un", PRECO);

        assertThat(produto.ativo()).isTrue();
    }

    @Test
    void rejeitaNcmComTamanhoErrado() {
        assertThatThrownBy(() -> new Produto("Caneca", null, "691200", "un", PRECO))
                .isInstanceOf(DomainException.class);
    }

    @Test
    void rejeitaNcmComLetras() {
        assertThatThrownBy(() -> new Produto("Caneca", null, "6912000A", "un", PRECO))
                .isInstanceOf(DomainException.class);
    }

    @Test
    void desativarTornaProdutoInativo() {
        Produto produto = new Produto("Caneca", null, "69120000", "un", PRECO);

        produto.desativar();

        assertThat(produto.ativo()).isFalse();
    }

    @Test
    void produtoNovoComecaSemEstoque() {
        assertThat(caneca().quantidadeEmEstoque()).isZero();
    }

    @Test
    void entradaSomaAoEstoqueEVendaSubtrai() {
        Produto produto = caneca();

        produto.darEntradaNoEstoque(10);
        produto.baixarDoEstoque(3);

        assertThat(produto.quantidadeEmEstoque()).isEqualTo(7);
    }

    @Test
    void naoVendeMaisDoQueTemEmEstoque() {
        Produto produto = caneca();
        produto.darEntradaNoEstoque(2);

        assertThatThrownBy(() -> produto.baixarDoEstoque(3))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("Estoque insuficiente");
        assertThat(produto.quantidadeEmEstoque()).isEqualTo(2);
    }

    @Test
    void devolucaoVoltaAoEstoque() {
        Produto produto = caneca();
        produto.darEntradaNoEstoque(5);
        produto.baixarDoEstoque(5);

        produto.devolverAoEstoque(2);

        assertThat(produto.quantidadeEmEstoque()).isEqualTo(2);
    }

    @Test
    void recusaQuantidadeZeroOuNegativa() {
        Produto produto = caneca();

        assertThatThrownBy(() -> produto.darEntradaNoEstoque(0)).isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> produto.darEntradaNoEstoque(-4)).isInstanceOf(DomainException.class);
    }

    @Test
    void aceitaCodigoDeBarrasEan13EEan8Validos() {
        assertThat(new Produto("Caneca", null, "69120000", "un", PRECO, "4006381333931").codigoBarras())
                .contains("4006381333931");
        assertThat(new Produto("Caneca", null, "69120000", "un", PRECO, "96385074").codigoBarras())
                .contains("96385074");
    }

    @Test
    void recusaCodigoDeBarrasComDigitoVerificadorErrado() {
        assertThatThrownBy(() -> new Produto("Caneca", null, "69120000", "un", PRECO, "4006381333932"))
                .isInstanceOf(DomainException.class);
    }

    @Test
    void codigoDeBarrasEmBrancoFicaVazio() {
        assertThat(new Produto("Caneca", null, "69120000", "un", PRECO, "  ").codigoBarras()).isEmpty();
    }

    @Test
    void atualizarComDadoInvalidoNaoAlteraNada() {
        Produto produto = caneca();

        assertThatThrownBy(() -> produto.atualizar("Novo nome", null, PRECO, "123"))
                .isInstanceOf(DomainException.class);
        assertThat(produto.nome()).isEqualTo("Caneca");
    }

    private static Produto caneca() {
        return new Produto("Caneca", null, "69120000", "un", PRECO);
    }
}

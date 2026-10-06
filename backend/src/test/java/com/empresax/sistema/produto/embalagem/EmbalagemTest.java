package com.empresax.sistema.produto.embalagem;

import com.empresax.sistema.common.domain.DomainException;
import com.empresax.sistema.pedido.ItemPedido;
import com.empresax.sistema.pedido.TipoItem;
import com.empresax.sistema.shared.dinheiro.Dinheiro;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EmbalagemTest {

    private static final UUID PRODUTO = UUID.randomUUID();

    @Test
    void fardoTemPrecoProprioECustoDasUnidades() {
        Embalagem fardo = new Embalagem(PRODUTO, " Fardo com 12 ", null, 12, dinheiro("42.00"));

        assertThat(fardo.nome()).isEqualTo("Fardo com 12");
        assertThat(fardo.custoA(dinheiro("2.50"))).isEqualTo(dinheiro("30.00"));
        assertThat(fardo.codigoBarras()).isEmpty();
    }

    @Test
    void embalagemComMenosDeDuasUnidadesOuSemPrecoEhRecusada() {
        assertThatThrownBy(() -> new Embalagem(PRODUTO, "Unidade", null, 1, dinheiro("3.50")))
                .isInstanceOf(DomainException.class).hasMessageContaining("entre 2 e 1000");
        assertThatThrownBy(() -> new Embalagem(PRODUTO, "Fardo", null, 12, Dinheiro.zero()))
                .hasMessageContaining("preço");
    }

    @Test
    void codigoDeBarrasDaEmbalagemPassaPelaMesmaConferenciaDoProduto() {
        assertThatThrownBy(() -> new Embalagem(PRODUTO, "Fardo", "7891000000015", 12, dinheiro("42.00")))
                .hasMessageContaining("Código de barras inválido");
        assertThat(new Embalagem(PRODUTO, "Fardo", "7891000000014", 12, dinheiro("42.00")).codigoBarras())
                .contains("7891000000014");
    }

    @Test
    void itemVendidoEmEmbalagemBaixaOEstoqueEmUnidades() {
        ItemPedido doisFardos = new ItemPedido(TipoItem.PRODUTO, PRODUTO, "Refrigerante — Fardo com 12", dinheiro("42.00"), 2);

        doisFardos.venderEmEmbalagem(12);

        assertThat(doisFardos.unidadesDoEstoque()).isEqualTo(24);
        assertThat(doisFardos.subtotal()).isEqualTo(dinheiro("84.00"));
    }

    @Test
    void itemAvulsoBaixaAPropriaQuantidade() {
        ItemPedido avulso = new ItemPedido(TipoItem.PRODUTO, PRODUTO, "Refrigerante", dinheiro("3.90"), 5);

        assertThat(avulso.unidadesDoEstoque()).isEqualTo(5);
    }

    private static Dinheiro dinheiro(String valor) {
        return new Dinheiro(new BigDecimal(valor));
    }
}

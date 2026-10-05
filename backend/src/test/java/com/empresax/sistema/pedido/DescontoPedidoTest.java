package com.empresax.sistema.pedido;

import com.empresax.sistema.common.domain.DomainException;
import com.empresax.sistema.shared.dinheiro.Dinheiro;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DescontoPedidoTest {

    private static final String GERENTE = "gerente@x.com";

    @Test
    void descontoEhRateadoNaProporcaoDoValorDeCadaItem() {
        // 30 + 70 = 100; desconto 10 → 3 e 7.
        Pedido pedido = venda(item("30.00", 1), item("35.00", 2));

        pedido.aplicarDesconto(dinheiro("10.00"), GERENTE);

        assertThat(pedido.itens()).extracting(ItemPedido::desconto).containsExactly(dinheiro("3.00"), dinheiro("7.00"));
        assertThat(pedido.valorTotal()).isEqualTo(dinheiro("90.00"));
        assertThat(pedido.desconto()).isEqualTo(dinheiro("10.00"));
        assertThat(pedido.descontoAutorizadoPor()).contains(GERENTE);
    }

    @Test
    void centavosDoArredondamentoSaoDistribuidosEASomaBateExatamente() {
        Pedido pedido = venda(item("10.00", 1), item("10.00", 1), item("10.00", 1));

        pedido.aplicarDesconto(dinheiro("1.00"), GERENTE);

        BigDecimal somaDosDescontos = pedido.itens().stream().map(item -> item.desconto().valor()).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(somaDosDescontos).isEqualByComparingTo("1.00");
        assertThat(pedido.valorTotal()).isEqualTo(dinheiro("29.00"));
    }

    @Test
    void descontoIgualOuMaiorQueATotalEhRecusado() {
        Pedido pedido = venda(item("20.00", 1));

        assertThatThrownBy(() -> pedido.aplicarDesconto(dinheiro("20.00"), GERENTE))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("maior que o total");
    }

    @Test
    void descontoSemAutorizacaoEhRecusado() {
        Pedido pedido = venda(item("20.00", 1));

        assertThatThrownBy(() -> pedido.aplicarDesconto(dinheiro("2.00"), " ")).hasMessageContaining("autorização");
    }

    @Test
    void pedidoJaConfirmadoNaoRecebeDesconto() {
        Pedido pedido = venda(item("20.00", 1));
        pedido.confirmar();

        assertThatThrownBy(() -> pedido.aplicarDesconto(dinheiro("2.00"), GERENTE)).hasMessageContaining("não está aberto");
    }

    private static Pedido venda(ItemPedido... itens) {
        return Pedido.noBalcao(List.of(itens), null, null, UUID.randomUUID());
    }

    private static ItemPedido item(String preco, int quantidade) {
        return new ItemPedido(TipoItem.PRODUTO, UUID.randomUUID(), "Produto", dinheiro(preco), quantidade);
    }

    private static Dinheiro dinheiro(String valor) {
        return new Dinheiro(new BigDecimal(valor));
    }
}

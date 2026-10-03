package com.empresax.sistema.pedido;

import com.empresax.sistema.common.domain.DomainException;
import com.empresax.sistema.documentofiscal.TipoDocumentoFiscal;
import com.empresax.sistema.shared.dinheiro.Dinheiro;
import com.empresax.sistema.shared.documento.Cpf;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PedidoTest {

    @Test
    void calculaValorTotalSomandoOsSubtotaisDosItens() {
        ItemPedido item1 = new ItemPedido(
                TipoItem.PRODUTO, UUID.randomUUID(), "Caneca", new Dinheiro(new BigDecimal("10.00")), 2);
        ItemPedido item2 = new ItemPedido(
                TipoItem.SERVICO, UUID.randomUUID(), "Consultoria", new Dinheiro(new BigDecimal("100.00")), 1);

        Pedido pedido = new Pedido(UUID.randomUUID(), List.of(item1, item2));

        assertThat(pedido.valorTotal().valor()).isEqualByComparingTo("120.00");
    }

    @Test
    void rejeitaPedidoSemItens() {
        assertThatThrownBy(() -> new Pedido(UUID.randomUUID(), List.of()))
                .isInstanceOf(DomainException.class);
    }

    @Test
    void confirmarMudaStatusParaAguardandoEmissao() {
        Pedido pedido = pedidoComUmItem();

        pedido.confirmar();

        assertThat(pedido.status()).isEqualTo(StatusPedido.AGUARDANDO_EMISSAO);
    }

    @Test
    void naoPermiteAdicionarItemAposConfirmar() {
        Pedido pedido = pedidoComUmItem();
        pedido.confirmar();

        ItemPedido novoItem = new ItemPedido(
                TipoItem.PRODUTO, UUID.randomUUID(), "Caneca", new Dinheiro(new BigDecimal("10.00")), 1);

        assertThatThrownBy(() -> pedido.adicionarItem(novoItem)).isInstanceOf(DomainException.class);
    }

    @Test
    void naoPermiteCancelarPedidoConcluido() {
        Pedido pedido = pedidoComUmItem();
        pedido.confirmar();
        pedido.concluir();

        assertThatThrownBy(pedido::cancelar).isInstanceOf(DomainException.class);
    }

    @Test
    void naoPermiteConcluirPedidoAindaAberto() {
        Pedido pedido = pedidoComUmItem();

        assertThatThrownBy(pedido::concluir).isInstanceOf(DomainException.class);
    }

    @Test
    void pedidoComProdutoEServicoExigeNfeENfse() {
        ItemPedido produto = new ItemPedido(
                TipoItem.PRODUTO, UUID.randomUUID(), "Caneca", new Dinheiro(new BigDecimal("10.00")), 1);
        ItemPedido servico = new ItemPedido(
                TipoItem.SERVICO, UUID.randomUUID(), "Personalização", new Dinheiro(new BigDecimal("20.00")), 1);

        Pedido pedido = new Pedido(UUID.randomUUID(), List.of(produto, servico));

        assertThat(pedido.documentosFiscaisNecessarios())
                .containsExactlyInAnyOrder(TipoDocumentoFiscal.NFE, TipoDocumentoFiscal.NFSE);
    }

    @Test
    void pedidoSoComProdutosExigeApenasNfe() {
        Pedido pedido = pedidoComUmItem();

        assertThat(pedido.documentosFiscaisNecessarios()).containsExactly(TipoDocumentoFiscal.NFE);
    }

    @Test
    void vendaNoBalcaoComProdutoExigeNfceENaoNfe() {
        ItemPedido caneca = new ItemPedido(
                TipoItem.PRODUTO, UUID.randomUUID(), "Caneca", new Dinheiro(new BigDecimal("10.00")), 1);
        ItemPedido arte = new ItemPedido(
                TipoItem.SERVICO, UUID.randomUUID(), "Arte", new Dinheiro(new BigDecimal("25.00")), 1);

        Pedido pedido = Pedido.noBalcao(List.of(caneca, arte), null, null);

        assertThat(pedido.documentosFiscaisNecessarios())
                .containsExactlyInAnyOrder(TipoDocumentoFiscal.NFCE, TipoDocumentoFiscal.NFSE);
    }

    @Test
    void vendaNoBalcaoNaoPrecisaDeClienteEGuardaOCpfNaNota() {
        Pedido pedido = Pedido.noBalcao(List.of(itemCaneca()), new Cpf("111.444.777-35"), null);

        assertThat(pedido.clienteId()).isEmpty();
        assertThat(pedido.vendidoNoBalcao()).isTrue();
        assertThat(pedido.cpfNaNota()).contains("11144477735");
    }

    @Test
    void pedidoForaDoBalcaoContinuaExigindoCliente() {
        assertThatThrownBy(() -> new Pedido(null, List.of(itemCaneca()))).isInstanceOf(DomainException.class);
    }

    private static ItemPedido itemCaneca() {
        return new ItemPedido(TipoItem.PRODUTO, UUID.randomUUID(), "Caneca", new Dinheiro(new BigDecimal("10.00")), 1);
    }

    private static Pedido pedidoComUmItem() {
        ItemPedido item = new ItemPedido(
                TipoItem.PRODUTO, UUID.randomUUID(), "Caneca", new Dinheiro(new BigDecimal("10.00")), 1);
        return new Pedido(UUID.randomUUID(), List.of(item));
    }
}

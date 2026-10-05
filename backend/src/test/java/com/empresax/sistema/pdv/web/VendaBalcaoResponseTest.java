package com.empresax.sistema.pdv.web;

import com.empresax.sistema.pdv.BandeiraCartao;
import com.empresax.sistema.pdv.FormaPagamentoPresencial;
import com.empresax.sistema.pdv.PagamentoPresencial;
import com.empresax.sistema.pdv.VendaBalcao;
import com.empresax.sistema.pedido.ItemPedido;
import com.empresax.sistema.pedido.Pedido;
import com.empresax.sistema.pedido.TipoItem;
import com.empresax.sistema.shared.dinheiro.Dinheiro;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class VendaBalcaoResponseTest {

    private static final String OPERADOR = "caixa1@x.com";
    private static final UUID PEDIDO_ID = UUID.randomUUID();

    private final Pedido pedido = Pedido.noBalcao(
            List.of(new ItemPedido(TipoItem.PRODUTO, UUID.randomUUID(), "Arroz", dinheiro("50.00"), 1)), null, null, UUID.randomUUID());

    @Test
    void vendaDivididaMostraCadaParteEOTrocoDoDinheiro() {
        PagamentoPresencial cartao = PagamentoPresencial.naMaquininhaAvulsa(
                PEDIDO_ID, FormaPagamentoPresencial.CARTAO_DEBITO, dinheiro("30.00"), BandeiraCartao.values()[0], "123456", OPERADOR);
        PagamentoPresencial especie = PagamentoPresencial.emDinheiro(PEDIDO_ID, dinheiro("20.00"), dinheiro("50.00"), OPERADOR);

        VendaBalcaoResponse resposta = VendaBalcaoResponse.de(VendaBalcao.presencial(pedido, List.of(cartao, especie)), Map.of());

        assertThat(resposta.formaPagamento()).isEqualTo("DIVIDIDO");
        assertThat(resposta.statusPagamento()).isEqualTo("APROVADO");
        assertThat(resposta.troco()).isEqualByComparingTo("30.00");
        assertThat(resposta.codigoAutorizacao()).isEqualTo("123456");
        assertThat(resposta.pagamentos()).extracting(VendaBalcaoResponse.ParteResponse::forma)
                .containsExactly("CARTAO_DEBITO", "DINHEIRO");
    }

    @Test
    void vendaDivididaFicaAguardandoEnquantoAMaquininhaNaoAprovaORestante() {
        PagamentoPresencial especie = PagamentoPresencial.emDinheiro(PEDIDO_ID, dinheiro("20.00"), dinheiro("20.00"), OPERADOR);
        PagamentoPresencial restante = PagamentoPresencial.aguardandoMaquininha(
                PEDIDO_ID, FormaPagamentoPresencial.CARTAO_CREDITO, dinheiro("30.00"), "tx-1", OPERADOR);

        VendaBalcaoResponse resposta = VendaBalcaoResponse.de(VendaBalcao.presencial(pedido, List.of(especie, restante)), Map.of());

        assertThat(resposta.statusPagamento()).isEqualTo("AGUARDANDO");
        assertThat(resposta.maquininhaIntegrada()).isTrue();
    }

    @Test
    void pagamentoNumaFormaSoContinuaComAFormaDele() {
        PagamentoPresencial especie = PagamentoPresencial.emDinheiro(PEDIDO_ID, dinheiro("50.00"), dinheiro("50.00"), OPERADOR);

        VendaBalcaoResponse resposta = VendaBalcaoResponse.de(VendaBalcao.presencial(pedido, List.of(especie)), Map.of());

        assertThat(resposta.formaPagamento()).isEqualTo("DINHEIRO");
        assertThat(resposta.pagamentos()).hasSize(1);
    }

    private static Dinheiro dinheiro(String valor) {
        return new Dinheiro(new BigDecimal(valor));
    }
}

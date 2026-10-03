package com.empresax.sistema.pdv;

import com.empresax.sistema.common.domain.DomainException;
import com.empresax.sistema.shared.dinheiro.Dinheiro;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PagamentoPresencialTest {

    private static final UUID PEDIDO = UUID.randomUUID();
    private static final Dinheiro TOTAL = dinheiro("39.90");
    private static final String OPERADOR = "caixa@empresax.com";

    @Test
    void dinheiroCalculaOTroco() {
        PagamentoPresencial pagamento = PagamentoPresencial.emDinheiro(PEDIDO, TOTAL, dinheiro("50.00"), OPERADOR);

        assertThat(pagamento.troco()).contains(dinheiro("10.10"));
    }

    @Test
    void dinheiroRecebidoMenorQueOTotalEhRecusado() {
        assertThatThrownBy(() -> PagamentoPresencial.emDinheiro(PEDIDO, TOTAL, dinheiro("20.00"), OPERADOR))
                .isInstanceOf(DomainException.class);
    }

    @Test
    void cartaoExigeBandeiraECodigoDeAutorizacao() {
        assertThatThrownBy(() -> PagamentoPresencial.naMaquininhaAvulsa(
                PEDIDO, FormaPagamentoPresencial.CARTAO_CREDITO, TOTAL, null, "123456", OPERADOR))
                .hasMessageContaining("bandeira");
        assertThatThrownBy(() -> PagamentoPresencial.naMaquininhaAvulsa(
                PEDIDO, FormaPagamentoPresencial.CARTAO_DEBITO, TOTAL, BandeiraCartao.ELO, " ", OPERADOR))
                .hasMessageContaining("autorização");
    }

    @Test
    void cartaoNaMaquininhaAvulsaFicaMarcadoComoNaoIntegrado() {
        PagamentoPresencial pagamento = PagamentoPresencial.naMaquininhaAvulsa(
                PEDIDO, FormaPagamentoPresencial.CARTAO_CREDITO, TOTAL, BandeiraCartao.VISA, "a1b2c3", OPERADOR);

        assertThat(pagamento.maquininhaIntegrada()).isFalse();
        assertThat(pagamento.codigoAutorizacao()).contains("A1B2C3");
        assertThat(pagamento.troco()).isEmpty();
    }

    @Test
    void pixNaoGuardaBandeira() {
        PagamentoPresencial pagamento = PagamentoPresencial.naMaquininhaAvulsa(
                PEDIDO, FormaPagamentoPresencial.PIX, TOTAL, BandeiraCartao.VISA, "E2E12345", OPERADOR);

        assertThat(pagamento.bandeira()).isEmpty();
    }

    @Test
    void dinheiroNaoPassaPelaMaquininha() {
        assertThatThrownBy(() -> PagamentoPresencial.naMaquininhaAvulsa(
                PEDIDO, FormaPagamentoPresencial.DINHEIRO, TOTAL, null, "123456", OPERADOR))
                .isInstanceOf(DomainException.class);
    }

    @Test
    void estornoAconteceUmaVezSo() {
        PagamentoPresencial pagamento = PagamentoPresencial.emDinheiro(PEDIDO, TOTAL, TOTAL, OPERADOR);

        pagamento.estornar();

        assertThat(pagamento.status()).isEqualTo(StatusPagamentoPresencial.ESTORNADO);
        assertThatThrownBy(pagamento::estornar).isInstanceOf(DomainException.class);
    }

    @Test
    void maquininhaIntegradaAprovaComDadosVindosDaMaquininha() {
        PagamentoPresencial pagamento = PagamentoPresencial.aguardandoMaquininha(
                PEDIDO, FormaPagamentoPresencial.CARTAO_CREDITO, TOTAL, "intent-1", OPERADOR);

        pagamento.confirmarPelaMaquininha(BandeiraCartao.MASTERCARD, "778899", "pay-1");

        assertThat(pagamento.aprovado()).isTrue();
        assertThat(pagamento.maquininhaIntegrada()).isTrue();
        assertThat(pagamento.bandeira()).contains(BandeiraCartao.MASTERCARD);
        assertThat(pagamento.idPagamentoProvedor()).contains("pay-1");
    }

    @Test
    void cartaoRecusadoPermiteNovaTentativa() {
        PagamentoPresencial pagamento = PagamentoPresencial.aguardandoMaquininha(
                PEDIDO, FormaPagamentoPresencial.CARTAO_DEBITO, TOTAL, "intent-1", OPERADOR);

        pagamento.recusarPelaMaquininha();
        pagamento.novaTentativaNaMaquininha("intent-2");

        assertThat(pagamento.aguardandoMaquininha()).isTrue();
        assertThat(pagamento.idTransacaoMaquininha()).contains("intent-2");
    }

    @Test
    void desistenciaAntesDePagarNaoViraEstorno() {
        PagamentoPresencial pagamento = PagamentoPresencial.aguardandoMaquininha(
                PEDIDO, FormaPagamentoPresencial.CARTAO_CREDITO, TOTAL, "intent-1", OPERADOR);

        pagamento.cancelarAntesDoPagamento();

        assertThat(pagamento.status()).isEqualTo(StatusPagamentoPresencial.CANCELADO);
        assertThatThrownBy(pagamento::estornar).isInstanceOf(DomainException.class);
    }

    @Test
    void maquininhaIntegradaNaoCobraPix() {
        assertThatThrownBy(() -> PagamentoPresencial.aguardandoMaquininha(
                PEDIDO, FormaPagamentoPresencial.PIX, TOTAL, "intent-1", OPERADOR))
                .isInstanceOf(DomainException.class);
    }

    private static Dinheiro dinheiro(String valor) {
        return new Dinheiro(new BigDecimal(valor));
    }
}

package com.empresax.sistema.cobranca;

import com.empresax.sistema.cobranca.pagamento.StatusPagamentoExterno;
import com.empresax.sistema.common.domain.DomainException;
import com.empresax.sistema.shared.dinheiro.Dinheiro;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CobrancaTest {

    private static final Dinheiro VALOR = new Dinheiro(new BigDecimal("150.00"));

    @Test
    void nasceComStatusPendente() {
        Cobranca cobranca = new Cobranca(UUID.randomUUID(), MeioCobranca.PIX, VALOR, "ref-externa-1");

        assertThat(cobranca.status()).isEqualTo(StatusCobranca.PENDENTE);
    }

    @Test
    void marcarComoPagaMudaStatus() {
        Cobranca cobranca = new Cobranca(UUID.randomUUID(), MeioCobranca.PIX, VALOR, "ref-externa-2");

        cobranca.marcarComoPaga();

        assertThat(cobranca.status()).isEqualTo(StatusCobranca.PAGA);
    }

    @Test
    void naoPermiteMarcarComoPagaDuasVezes() {
        Cobranca cobranca = new Cobranca(UUID.randomUUID(), MeioCobranca.BOLETO, VALOR, "ref-externa-3");
        cobranca.marcarComoPaga();

        assertThatThrownBy(cobranca::marcarComoPaga).isInstanceOf(DomainException.class);
    }

    @Test
    void naoPermiteCancelarCobrancaJaPaga() {
        Cobranca cobranca = new Cobranca(UUID.randomUUID(), MeioCobranca.BOLETO, VALOR, "ref-externa-4");
        cobranca.marcarComoPaga();

        assertThatThrownBy(cobranca::cancelar).isInstanceOf(DomainException.class);
    }

    @Test
    void rejeitaReferenciaExternaEmBranco() {
        assertThatThrownBy(() -> new Cobranca(UUID.randomUUID(), MeioCobranca.PIX, VALOR, " "))
                .isInstanceOf(DomainException.class);
    }

    @Test
    void avisoDeAprovadoDoProvedorDaBaixaNaCobrancaPendente() {
        Cobranca cobranca = new Cobranca(UUID.randomUUID(), MeioCobranca.PIX, VALOR, "ref-externa-8");

        cobranca.aplicarStatusDoProvedor(StatusPagamentoExterno.APROVADO);

        assertThat(cobranca.status()).isEqualTo(StatusCobranca.PAGA);
    }

    @Test
    void avisoAtrasadoDoProvedorNaoDesfazReembolso() {
        Cobranca cobranca = new Cobranca(UUID.randomUUID(), MeioCobranca.PIX, VALOR, "ref-externa-9");
        cobranca.marcarComoPaga();
        cobranca.marcarComoReembolsada();

        cobranca.aplicarStatusDoProvedor(StatusPagamentoExterno.REJEITADO);

        assertThat(cobranca.status()).isEqualTo(StatusCobranca.REEMBOLSADA);
    }

    @Test
    void cobrancaPagaPodeSerReembolsadaUmaVezSo() {
        Cobranca cobranca = new Cobranca(UUID.randomUUID(), MeioCobranca.PIX, VALOR, "ref-externa-5");
        cobranca.marcarComoPaga();

        cobranca.marcarComoReembolsada();

        assertThat(cobranca.status()).isEqualTo(StatusCobranca.REEMBOLSADA);
        assertThatThrownBy(cobranca::marcarComoReembolsada).isInstanceOf(DomainException.class);
    }

    @Test
    void naoReembolsaCobrancaQueNaoFoiPaga() {
        Cobranca cobranca = new Cobranca(UUID.randomUUID(), MeioCobranca.PIX, VALOR, "ref-externa-6");

        assertThatThrownBy(cobranca::marcarComoReembolsada).isInstanceOf(DomainException.class);
    }

    @Test
    void cobrancaReembolsadaNaoViraCancelada() {
        Cobranca cobranca = new Cobranca(UUID.randomUUID(), MeioCobranca.PIX, VALOR, "ref-externa-7");
        cobranca.marcarComoPaga();
        cobranca.marcarComoReembolsada();

        assertThatThrownBy(cobranca::cancelar).isInstanceOf(DomainException.class);
        assertThat(cobranca.status()).isEqualTo(StatusCobranca.REEMBOLSADA);
    }
}

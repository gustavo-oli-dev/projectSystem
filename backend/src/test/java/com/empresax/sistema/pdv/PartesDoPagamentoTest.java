package com.empresax.sistema.pdv;

import com.empresax.sistema.common.domain.DomainException;
import com.empresax.sistema.shared.dinheiro.Dinheiro;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PartesDoPagamentoTest {

    @Test
    void restanteEhOTotalMenosAsPartesJaRecebidas() {
        PartesDoPagamento partes = new PartesDoPagamento(List.of(
                parte(FormaPagamentoPresencial.DINHEIRO, "20.00"),
                parte(FormaPagamentoPresencial.CARTAO_DEBITO, "30.50")));

        assertThat(partes.restante(dinheiro("100.00"))).isEqualTo(dinheiro("49.50"));
    }

    @Test
    void semPartesORestanteEhOTotalInteiro() {
        assertThat(PartesDoPagamento.nenhuma().restante(dinheiro("12.90"))).isEqualTo(dinheiro("12.90"));
        assertThat(new PartesDoPagamento(null).vazia()).isTrue();
    }

    @Test
    void partesQueJaPagamOTotalSaoRecusadasPoisAUltimaFormaPrecisaPagarAlgo() {
        PartesDoPagamento partes = new PartesDoPagamento(List.of(parte(FormaPagamentoPresencial.DINHEIRO, "50.00")));

        assertThatThrownBy(() -> partes.restante(dinheiro("50.00")))
                .isInstanceOf(DomainException.class).hasMessageContaining("precisa pagar o que falta");
    }

    @Test
    void parteSemValorOuSemFormaEhRecusada() {
        assertThatThrownBy(() -> new PartesDoPagamento(List.of(parte(FormaPagamentoPresencial.DINHEIRO, "0.00"))))
                .hasMessageContaining("valor maior que zero");
        assertThatThrownBy(() -> new PartesDoPagamento(List.of(parte(null, "5.00"))))
                .hasMessageContaining("forma de cada parte");
    }

    @Test
    void maisPartesQueOLimiteSaoRecusadas() {
        List<DadosPagamentoPresencial> demais = Collections.nCopies(
                PartesDoPagamento.MAXIMO_DE_PARTES + 1, parte(FormaPagamentoPresencial.DINHEIRO, "1.00"));

        assertThatThrownBy(() -> new PartesDoPagamento(demais)).hasMessageContaining("no máximo");
    }

    private static DadosPagamentoPresencial parte(FormaPagamentoPresencial forma, String valor) {
        return new DadosPagamentoPresencial(forma, new BigDecimal(valor), null, BandeiraCartao.values()[0], "123456");
    }

    private static Dinheiro dinheiro(String valor) {
        return new Dinheiro(new BigDecimal(valor));
    }
}

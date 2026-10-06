package com.empresax.sistema.produto.fiscal;

import com.empresax.sistema.common.domain.DomainException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TributacaoProdutoTest {

    @Test
    void produtoTributadoNormalmenteGuardaCstAliquotaEClassificacao() {
        TributacaoProduto arroz = new TributacaoProduto(0, "00", new BigDecimal("7.00"), false, null, true, "200034");

        assertThat(arroz.cstIcms()).isEqualTo("00");
        assertThat(arroz.cestaBasica()).isTrue();
        assertThat(arroz.doSimplesNacional()).isFalse();
        assertThat(arroz.classificacaoTributaria()).contains("200034");
    }

    @Test
    void csosnDeTresDigitosIndicaSimplesNacional() {
        assertThat(new TributacaoProduto(0, "102", null, false, null, false, null).doSimplesNacional()).isTrue();
    }

    @Test
    void substituicaoTributariaExigeCestECstDeSt() {
        assertThatThrownBy(() -> new TributacaoProduto(0, "60", null, true, null, false, null))
                .isInstanceOf(DomainException.class).hasMessageContaining("precisa do CEST");
        assertThatThrownBy(() -> new TributacaoProduto(0, "00", null, true, "0300100", false, null))
                .hasMessageContaining("CST de ST");

        TributacaoProduto refrigerante = new TributacaoProduto(0, "500", null, true, "0300100", false, null);

        assertThat(refrigerante.substituicaoTributaria()).isTrue();
        assertThat(refrigerante.cest()).contains("0300100");
    }

    @Test
    void codigosComTamanhoErradoSaoRecusados() {
        assertThatThrownBy(() -> new TributacaoProduto(0, "1", null, false, null, false, null)).hasMessageContaining("CST");
        assertThatThrownBy(() -> new TributacaoProduto(0, "00", null, false, "123", false, null)).hasMessageContaining("7 dígitos");
        assertThatThrownBy(() -> new TributacaoProduto(0, "00", null, false, null, false, "12")).hasMessageContaining("6 dígitos");
        assertThatThrownBy(() -> new TributacaoProduto(9, "00", null, false, null, false, null)).hasMessageContaining("0 (nacional) a 8");
        assertThatThrownBy(() -> new TributacaoProduto(0, "00", new BigDecimal("101"), false, null, false, null))
                .hasMessageContaining("entre 0% e 100%");
    }
}

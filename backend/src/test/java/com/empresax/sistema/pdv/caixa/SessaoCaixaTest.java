package com.empresax.sistema.pdv.caixa;

import com.empresax.sistema.common.domain.DomainException;
import com.empresax.sistema.shared.dinheiro.Dinheiro;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SessaoCaixaTest {

    private static final String OPERADOR = "caixa1@empresax.com";
    private static final String GERENTE = "gerente@empresax.com";
    /** 10 × R$ 2 + 10 × R$ 5 + 20 moedas de R$ 1 = R$ 90,00. */
    private static final ContagemCedulas FUNDO = new ContagemCedulas(Map.of(
            Cedula.NOTA_2, 10, Cedula.NOTA_5, 10, Cedula.MOEDA_1_REAL, 20));

    @Test
    void abreComOFundoDeTrocoSomadoPelasCedulas() {
        SessaoCaixa caixa = SessaoCaixa.abrir(OPERADOR, FUNDO);

        assertThat(caixa.aberta()).isTrue();
        assertThat(caixa.fundoInicial()).isEqualTo(dinheiro("90.00"));
        assertThat(caixa.cedulasAbertura().quantidades()).containsEntry(Cedula.NOTA_5, 10);
    }

    @Test
    void fechamentoQueBateNaoTemDiferencaEMostraOQueEntrou() {
        SessaoCaixa caixa = SessaoCaixa.abrir(OPERADOR, FUNDO);

        // R$ 90 de fundo + R$ 150 vendidos em dinheiro = R$ 240 na gaveta.
        caixa.fechar(new ContagemCedulas(Map.of(Cedula.NOTA_100, 2, Cedula.NOTA_20, 2)), dinheiro("150.00"), null);

        assertThat(caixa.aberta()).isFalse();
        assertThat(caixa.valorEsperado()).contains(new BigDecimal("240.00"));
        assertThat(caixa.diferenca()).contains(new BigDecimal("0.00"));
        assertThat(caixa.dinheiroQueEntrou()).contains(new BigDecimal("150.00"));
    }

    @Test
    void faltaDeDinheiroNaGavetaAparecePorDiferencaNegativa() {
        SessaoCaixa caixa = SessaoCaixa.abrir(OPERADOR, FUNDO);

        caixa.fechar(new ContagemCedulas(Map.of(Cedula.NOTA_100, 2, Cedula.NOTA_10, 3)), dinheiro("150.00"), "Conferir com o gerente");

        assertThat(caixa.diferenca()).contains(new BigDecimal("-10.00"));
        assertThat(caixa.observacaoFechamento()).contains("Conferir com o gerente");
    }

    @Test
    void reposicaoDeTrocoESangriaEntramNoValorEsperado() {
        SessaoCaixa caixa = SessaoCaixa.abrir(OPERADOR, FUNDO);

        caixa.registrarSuprimento(new ContagemCedulas(Map.of(Cedula.NOTA_2, 25)), "Reposição de troco", GERENTE);
        caixa.registrarSangria(dinheiro("100.00"), "Cofre", OPERADOR, dinheiro("200.00"));

        // 90 + 200 vendas + 50 reposição − 100 sangria = 240.
        assertThat(caixa.dinheiroEsperado(dinheiro("200.00"))).isEqualByComparingTo("240.00");
        assertThat(caixa.totalSuprimentos()).isEqualTo(dinheiro("50.00"));
        assertThat(caixa.totalSangrias()).isEqualTo(dinheiro("100.00"));
        assertThat(caixa.movimentos()).hasSize(2);
    }

    @Test
    void oQueEntrouDesconsideraReposicaoESomaSangria() {
        SessaoCaixa caixa = SessaoCaixa.abrir(OPERADOR, FUNDO);
        caixa.registrarSuprimento(new ContagemCedulas(Map.of(Cedula.NOTA_10, 5)), "Reposição de troco", GERENTE);
        caixa.registrarSangria(dinheiro("100.00"), "Cofre", OPERADOR, dinheiro("120.00"));

        // Gaveta: 90 + 120 + 50 − 100 = 160.
        caixa.fechar(new ContagemCedulas(Map.of(Cedula.NOTA_100, 1, Cedula.NOTA_50, 1, Cedula.NOTA_10, 1)), dinheiro("120.00"), null);

        assertThat(caixa.dinheiroQueEntrou()).contains(new BigDecimal("120.00"));
    }

    @Test
    void sangriaMaiorQueODinheiroDaGavetaEhRecusada() {
        SessaoCaixa caixa = SessaoCaixa.abrir(OPERADOR, FUNDO);

        assertThatThrownBy(() -> caixa.registrarSangria(dinheiro("100.00"), "Cofre", OPERADOR, dinheiro("5.00")))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("maior do que o dinheiro");
    }

    @Test
    void sangriaExigeMotivoEValorPositivo() {
        SessaoCaixa caixa = SessaoCaixa.abrir(OPERADOR, FUNDO);

        assertThatThrownBy(() -> caixa.registrarSangria(dinheiro("10.00"), " ", OPERADOR, Dinheiro.zero()))
                .hasMessageContaining("motivo");
        assertThatThrownBy(() -> caixa.registrarSangria(Dinheiro.zero(), "Cofre", OPERADOR, Dinheiro.zero()))
                .hasMessageContaining("maior que zero");
    }

    @Test
    void reposicaoSemCedulasEhRecusada() {
        SessaoCaixa caixa = SessaoCaixa.abrir(OPERADOR, FUNDO);

        assertThatThrownBy(() -> caixa.registrarSuprimento(ContagemCedulas.vazia(), "Troco", GERENTE))
                .hasMessageContaining("cédulas");
    }

    @Test
    void caixaFechadoNaoAceitaMaisMovimentoNemNovoFechamento() {
        SessaoCaixa caixa = SessaoCaixa.abrir(OPERADOR, FUNDO);
        caixa.fechar(FUNDO, Dinheiro.zero(), null);

        assertThatThrownBy(() -> caixa.registrarSuprimento(FUNDO, "Troco", GERENTE)).hasMessageContaining("fechado");
        assertThatThrownBy(() -> caixa.fechar(FUNDO, Dinheiro.zero(), null)).hasMessageContaining("fechado");
    }

    @Test
    void caixaPrecisaDeOperador() {
        assertThatThrownBy(() -> SessaoCaixa.abrir(" ", FUNDO)).isInstanceOf(DomainException.class);
    }

    private static Dinheiro dinheiro(String valor) {
        return new Dinheiro(new BigDecimal(valor));
    }
}

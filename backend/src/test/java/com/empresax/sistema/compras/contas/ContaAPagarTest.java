package com.empresax.sistema.compras.contas;

import com.empresax.sistema.common.domain.DomainException;
import com.empresax.sistema.compras.contato.Contato;
import com.empresax.sistema.compras.contato.TipoContato;
import com.empresax.sistema.shared.dinheiro.Dinheiro;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ContaAPagarTest {

    private static final Dinheiro VALOR = new Dinheiro(new BigDecimal("350.00"));
    private static final LocalDate VENCIMENTO = LocalDate.of(2026, 10, 10);

    @Test
    void contaPagaGuardaQuemPagouENaoPodeSerPagaDeNovo() {
        ContaAPagar conta = ContaAPagar.lancar(null, "Conta de luz", VALOR, VENCIMENTO, "gerente@x.com");

        conta.pagar("financeiro@x.com");

        assertThat(conta.status()).isEqualTo(StatusContaAPagar.PAGA);
        assertThat(conta.pagaPor()).contains("financeiro@x.com");
        assertThat(conta.pagaEm()).isPresent();
        assertThatThrownBy(() -> conta.pagar("financeiro@x.com")).hasMessageContaining("já foi paga");
    }

    @Test
    void vencidaEhSoAContaAbertaComVencimentoPassado() {
        ContaAPagar conta = ContaAPagar.lancar(null, "Aluguel", VALOR, VENCIMENTO, "gerente@x.com");

        assertThat(conta.vencida(VENCIMENTO)).isFalse();
        assertThat(conta.vencida(VENCIMENTO.plusDays(1))).isTrue();
        conta.pagar("financeiro@x.com");
        assertThat(conta.vencida(VENCIMENTO.plusDays(1))).isFalse();
    }

    @Test
    void contaSemValorOuSemDescricaoEhRecusada() {
        assertThatThrownBy(() -> ContaAPagar.lancar(null, "Luz", Dinheiro.zero(), VENCIMENTO, "g@x.com"))
                .isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> ContaAPagar.lancar(null, " ", VALOR, VENCIMENTO, "g@x.com"))
                .hasMessageContaining("descrição");
    }

    @Test
    void parcelaDaNotaExigeFornecedorENota() {
        assertThatThrownBy(() -> ContaAPagar.parcelaDaNota(UUID.randomUUID(), null, "NF 1", VALOR, VENCIMENTO, "g@x.com"))
                .hasMessageContaining("fornecedor e da nota");
    }

    @Test
    void contatoValidaOCnpjEGuardaSemPontuacao() {
        Contato fornecedor = new Contato(TipoContato.FORNECEDOR, "Distribuidora", "11.222.333/0001-81", null, null, null);

        assertThat(fornecedor.documento()).contains("11222333000181");
        assertThatThrownBy(() -> new Contato(TipoContato.FORNECEDOR, "X", "11.222.333/0001-00", null, null, null))
                .isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> new Contato(TipoContato.OUTRO, "Y", null, null, "sem-arroba", null))
                .hasMessageContaining("E-mail");
    }
}

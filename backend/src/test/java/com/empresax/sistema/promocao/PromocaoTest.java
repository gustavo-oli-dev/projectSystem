package com.empresax.sistema.promocao;

import com.empresax.sistema.common.domain.DomainException;
import com.empresax.sistema.pedido.ItemPedido;
import com.empresax.sistema.pedido.Pedido;
import com.empresax.sistema.pedido.TipoItem;
import com.empresax.sistema.shared.dinheiro.Dinheiro;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PromocaoTest {

    private static final UUID PRODUTO = UUID.randomUUID();
    private static final LocalDate HOJE = LocalDate.of(2026, 10, 6);
    private static final String GERENTE = "gerente@x.com";

    @Test
    void precoDeOfertaDaODescontoDaDiferencaEmCadaUnidade() {
        Promocao oferta = Promocao.precoDeOferta(PRODUTO, dinheiro("7.90"), dinheiro("9.90"), HOJE, HOJE.plusDays(6), GERENTE);

        assertThat(oferta.descontoPara(dinheiro("9.90"), 3)).isEqualTo(dinheiro("6.00"));
    }

    @Test
    void ofertaQueNaoEhMenorQueOPrecoAtualEhRecusada() {
        assertThatThrownBy(() -> Promocao.precoDeOferta(PRODUTO, dinheiro("9.90"), dinheiro("9.90"), HOJE, HOJE, GERENTE))
                .isInstanceOf(DomainException.class).hasMessageContaining("menor que o preço atual");
    }

    @Test
    void levePagueSoDaUnidadeDeGracaACadaGrupoCompleto() {
        Promocao leve3Pague2 = Promocao.levePague(PRODUTO, 3, 2, HOJE, HOJE, GERENTE);

        assertThat(leve3Pague2.descontoPara(dinheiro("5.00"), 2)).isEqualTo(Dinheiro.zero());
        assertThat(leve3Pague2.descontoPara(dinheiro("5.00"), 3)).isEqualTo(dinheiro("5.00"));
        assertThat(leve3Pague2.descontoPara(dinheiro("5.00"), 7)).isEqualTo(dinheiro("10.00"));
    }

    @Test
    void levePagueComLeveMenorOuIgualAPagueEhRecusado() {
        assertThatThrownBy(() -> Promocao.levePague(PRODUTO, 2, 2, HOJE, HOJE, GERENTE)).hasMessageContaining("leve precisa ser maior");
    }

    @Test
    void valeSoDentroDoPeriodoEAteSerEncerrada() {
        Promocao promocao = Promocao.levePague(PRODUTO, 3, 2, HOJE, HOJE.plusDays(2), GERENTE);

        assertThat(promocao.valeNoDia(HOJE.minusDays(1))).isFalse();
        assertThat(promocao.valeNoDia(HOJE.plusDays(2))).isTrue();
        assertThat(promocao.situacao(HOJE)).isEqualTo(SituacaoPromocao.VALENDO);

        promocao.encerrar(GERENTE, HOJE);

        assertThat(promocao.valeNoDia(HOJE)).isFalse();
        assertThat(promocao.situacao(HOJE)).isEqualTo(SituacaoPromocao.ENCERRADA);
    }

    @Test
    void ultimoDiaAntesDoPrimeiroEhRecusado() {
        assertThatThrownBy(() -> Promocao.levePague(PRODUTO, 3, 2, HOJE, HOJE.minusDays(1), GERENTE))
                .hasMessageContaining("não pode ser antes");
    }

    @Test
    void descontoDoGerenteEhRateadoSobreOValorJaComPromocaoESomaComEla() {
        ItemPedido comPromocao = new ItemPedido(TipoItem.PRODUTO, PRODUTO, "Arroz", dinheiro("10.00"), 3);
        comPromocao.receberPromocao(dinheiro("10.00"));
        ItemPedido semPromocao = new ItemPedido(TipoItem.PRODUTO, UUID.randomUUID(), "Feijão", dinheiro("20.00"), 1);
        Pedido pedido = Pedido.noBalcao(List.of(comPromocao, semPromocao), null, null, UUID.randomUUID());

        pedido.aplicarDesconto(dinheiro("4.00"), GERENTE);

        assertThat(pedido.descontoPromocao()).isEqualTo(dinheiro("10.00"));
        assertThat(pedido.desconto()).isEqualTo(dinheiro("4.00"));
        assertThat(comPromocao.desconto()).isEqualTo(dinheiro("12.00"));
        assertThat(pedido.valorTotal()).isEqualTo(dinheiro("36.00"));
    }

    private static Dinheiro dinheiro(String valor) {
        return new Dinheiro(new BigDecimal(valor));
    }
}

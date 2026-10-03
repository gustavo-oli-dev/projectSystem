package com.empresax.sistema.venda;

import com.empresax.sistema.cobranca.Cobranca;
import com.empresax.sistema.cobranca.CobrancaRepository;
import com.empresax.sistema.cobranca.MeioCobranca;
import com.empresax.sistema.cobranca.StatusCobranca;
import com.empresax.sistema.cobranca.pagamento.ProvedorPagamento;
import com.empresax.sistema.common.domain.DomainException;
import com.empresax.sistema.documentofiscal.DocumentoFiscalService;
import com.empresax.sistema.pedido.ItemPedido;
import com.empresax.sistema.pedido.Pedido;
import com.empresax.sistema.pedido.PedidoService;
import com.empresax.sistema.pedido.StatusPedido;
import com.empresax.sistema.pedido.TipoItem;
import com.empresax.sistema.produto.estoque.EstoqueService;
import com.empresax.sistema.shared.dinheiro.Dinheiro;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CancelamentoVendaServiceTest {

    private static final UUID PEDIDO_ID = UUID.randomUUID();
    private static final UUID PRODUTO_ID = UUID.randomUUID();
    private static final Dinheiro PRECO = new Dinheiro(new BigDecimal("39.90"));
    private static final String RESPONSAVEL = "gerente@empresax.com";

    private final PedidoService pedidoService = mock(PedidoService.class);
    private final CobrancaRepository cobrancaRepository = mock(CobrancaRepository.class);
    private final ProvedorPagamento provedorPagamento = mock(ProvedorPagamento.class);
    private final EstoqueService estoqueService = mock(EstoqueService.class);
    private final DocumentoFiscalService documentoFiscalService = mock(DocumentoFiscalService.class);
    private final CancelamentoVendaService servico = new CancelamentoVendaService(
            pedidoService, cobrancaRepository, provedorPagamento, estoqueService, documentoFiscalService);

    @Test
    void cancelarPedidoConfirmadoDevolveOsProdutosAoEstoque() {
        Pedido pedido = pedidoComDuasCanecas();
        pedido.confirmar();
        prepararPedido(pedido, List.of());

        servico.cancelar(PEDIDO_ID, RESPONSAVEL);

        assertThat(pedido.status()).isEqualTo(StatusPedido.CANCELADO);
        verify(estoqueService).devolverPorCancelamento(PRODUTO_ID, 2, pedido.id(), RESPONSAVEL);
    }

    @Test
    void cancelarPedidoAindaAbertoNaoMexeNoEstoque() {
        Pedido pedido = pedidoComDuasCanecas();
        prepararPedido(pedido, List.of());

        servico.cancelar(PEDIDO_ID, RESPONSAVEL);

        verify(estoqueService, never()).devolverPorCancelamento(any(), anyInt(), any(), anyString());
    }

    @Test
    void naoCancelaPedidoPagoSemReembolsar() {
        Pedido pedido = pedidoComDuasCanecas();
        pedido.confirmar();
        prepararPedido(pedido, List.of(cobrancaPaga()));

        assertThatThrownBy(() -> servico.cancelar(PEDIDO_ID, RESPONSAVEL))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("Reembolsar");
        assertThat(pedido.status()).isEqualTo(StatusPedido.AGUARDANDO_EMISSAO);
    }

    @Test
    void reembolsarDevolveODinheiroCancelaEDevolveOEstoque() {
        Pedido pedido = pedidoComDuasCanecas();
        pedido.confirmar();
        Cobranca paga = cobrancaPaga();
        prepararPedido(pedido, List.of(paga));

        servico.reembolsar(PEDIDO_ID, RESPONSAVEL);

        verify(provedorPagamento).reembolsar(eq("mp-123"), anyString());
        assertThat(paga.status()).isEqualTo(StatusCobranca.REEMBOLSADA);
        assertThat(pedido.status()).isEqualTo(StatusPedido.CANCELADO);
        verify(estoqueService).devolverPorCancelamento(PRODUTO_ID, 2, pedido.id(), RESPONSAVEL);
    }

    @Test
    void cancelarAvisaOMercadoPagoParaOClienteNaoPagarDepois() {
        Pedido pedido = pedidoComDuasCanecas();
        Cobranca pendente = new Cobranca(PEDIDO_ID, MeioCobranca.PIX, PRECO, "mp-456");
        prepararPedido(pedido, List.of(pendente));

        servico.cancelar(PEDIDO_ID, RESPONSAVEL);

        verify(provedorPagamento).cancelarCobranca("mp-456");
        assertThat(pendente.status()).isEqualTo(StatusCobranca.CANCELADA);
    }

    @Test
    void reembolsarSemPagamentoEhRecusado() {
        prepararPedido(pedidoComDuasCanecas(), List.of());

        assertThatThrownBy(() -> servico.reembolsar(PEDIDO_ID, RESPONSAVEL)).isInstanceOf(DomainException.class);
        verify(provedorPagamento, never()).reembolsar(anyString(), anyString());
    }

    @Test
    void vendaDeBalcaoNaoSeCancelaPeloPedidoSemEstornarOPagamento() {
        Pedido balcao = Pedido.noBalcao(List.of(new ItemPedido(TipoItem.PRODUTO, PRODUTO_ID, "Caneca", PRECO, 1)), null);
        balcao.confirmar();
        prepararPedido(balcao, List.of());

        assertThatThrownBy(() -> servico.cancelar(PEDIDO_ID, RESPONSAVEL))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("Caixa");
        assertThat(balcao.status()).isEqualTo(StatusPedido.AGUARDANDO_EMISSAO);
    }

    private static Pedido pedidoComDuasCanecas() {
        return new Pedido(UUID.randomUUID(), List.of(new ItemPedido(TipoItem.PRODUTO, PRODUTO_ID, "Caneca", PRECO, 2)));
    }

    private static Cobranca cobrancaPaga() {
        Cobranca cobranca = new Cobranca(PEDIDO_ID, MeioCobranca.PIX, PRECO, "mp-123");
        cobranca.marcarComoPaga();
        return cobranca;
    }

    private void prepararPedido(Pedido pedido, List<Cobranca> cobrancas) {
        when(pedidoService.buscarPorId(PEDIDO_ID)).thenReturn(pedido);
        when(cobrancaRepository.findByPedidoId(PEDIDO_ID)).thenReturn(cobrancas);
    }
}

package com.empresax.sistema.produto.estoque;

import com.empresax.sistema.common.domain.DomainException;
import com.empresax.sistema.produto.Produto;
import com.empresax.sistema.produto.ProdutoRepository;
import com.empresax.sistema.produto.estoque.EstoqueService.AjusteInventario;
import com.empresax.sistema.shared.dinheiro.Dinheiro;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PerdaEInventarioTest {

    private static final String ESTOQUISTA = "estoque@empresax.com";
    private static final Dinheiro PRECO = new Dinheiro(new BigDecimal("9.90"));
    private static final Dinheiro CUSTO = new Dinheiro(new BigDecimal("4.00"));

    private final ProdutoRepository produtoRepository = mock(ProdutoRepository.class);
    private final MovimentacaoEstoqueRepository movimentacaoRepository = mock(MovimentacaoEstoqueRepository.class);
    private final EstoqueService estoqueService = new EstoqueService(produtoRepository, movimentacaoRepository);

    @Test
    void perdaTiraDoEstoqueERegistraMotivoECustoDaHora() {
        UUID id = produtoComEstoque(10);

        Produto produto = estoqueService.registrarPerda(id, 3, MotivoPerda.VENCIDO, null, ESTOQUISTA);

        assertThat(produto.quantidadeEmEstoque()).isEqualTo(7);
        MovimentacaoEstoque perda = movimentacaoSalva();
        assertThat(perda.tipo()).isEqualTo(TipoMovimentacaoEstoque.PERDA);
        assertThat(perda.motivo()).contains(MotivoPerda.VENCIDO);
        assertThat(perda.custoUnitario()).contains(CUSTO);
        assertThat(perda.saldoApos()).isEqualTo(7);
    }

    @Test
    void perdaMaiorQueOEstoqueEhRecusadaENaoDeixaONegativo() {
        UUID id = produtoComEstoque(2);

        assertThatThrownBy(() -> estoqueService.registrarPerda(id, 5, MotivoPerda.AVARIADO, null, ESTOQUISTA))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("Estoque insuficiente");
        verify(movimentacaoRepository, never()).save(any());
    }

    @Test
    void trocaComClienteTiraDoEstoqueSemPrecisarEscreverMotivo() {
        UUID id = produtoComEstoque(5);

        Produto produto = estoqueService.registrarPerda(id, 1, MotivoPerda.TROCA, null, ESTOQUISTA);

        assertThat(produto.quantidadeEmEstoque()).isEqualTo(4);
        assertThat(movimentacaoSalva().motivo()).contains(MotivoPerda.TROCA);
    }

    @Test
    void motivoOutroExigeObservacao() {
        UUID id = produtoComEstoque(5);

        assertThatThrownBy(() -> estoqueService.registrarPerda(id, 1, MotivoPerda.OUTRO, "  ", ESTOQUISTA))
                .hasMessageContaining("Escreva o motivo");
    }

    @Test
    void inventarioAcertaOEstoqueAoContadoERegistraSoQuemMudou() {
        UUID faltou = produtoComEstoque(10);
        UUID sobrou = produtoComEstoque(4);
        UUID igual = produtoComEstoque(7);
        Map<UUID, Integer> contagens = new LinkedHashMap<>();
        contagens.put(faltou, 8);
        contagens.put(sobrou, 6);
        contagens.put(igual, 7);

        List<AjusteInventario> ajustes = estoqueService.aplicarInventario(contagens, ESTOQUISTA);

        assertThat(ajustes).hasSize(3);
        assertThat(ajustes).filteredOn(ajuste -> ajuste.produtoId().equals(faltou))
                .singleElement().satisfies(ajuste -> assertThat(ajuste.diferenca()).isEqualTo(-2));
        assertThat(ajustes).filteredOn(ajuste -> ajuste.produtoId().equals(sobrou))
                .singleElement().satisfies(ajuste -> assertThat(ajuste.diferenca()).isEqualTo(2));
        ArgumentCaptor<MovimentacaoEstoque> salvas = ArgumentCaptor.forClass(MovimentacaoEstoque.class);
        verify(movimentacaoRepository, times(2)).save(salvas.capture());
        assertThat(salvas.getAllValues()).extracting(MovimentacaoEstoque::tipo)
                .containsExactlyInAnyOrder(TipoMovimentacaoEstoque.INVENTARIO_FALTA, TipoMovimentacaoEstoque.INVENTARIO_SOBRA);
    }

    @Test
    void contagemNegativaEhRecusada() {
        Produto produto = new Produto("Arroz", null, "10063021", "un", PRECO);

        assertThatThrownBy(() -> produto.ajustarAoContado(-1)).isInstanceOf(DomainException.class);
    }

    @Test
    void inventarioVazioEhRecusado() {
        assertThatThrownBy(() -> estoqueService.aplicarInventario(Map.of(), ESTOQUISTA)).hasMessageContaining("ao menos um");
    }

    @Test
    void entradaEmLoteSomaCadaProdutoLidoERegistraUmaMovimentacaoPorProduto() {
        UUID arroz = produtoComEstoque(10);
        UUID feijao = produtoComEstoque(1);

        List<Produto> atualizados = estoqueService.darEntradaEmLote(Map.of(arroz, 12, feijao, 6), ESTOQUISTA);

        assertThat(atualizados).extracting(Produto::quantidadeEmEstoque).containsExactlyInAnyOrder(22, 7);
        verify(movimentacaoRepository, times(2)).save(any());
    }

    @Test
    void entradaEmLoteVaziaEhRecusada() {
        assertThatThrownBy(() -> estoqueService.darEntradaEmLote(Map.of(), ESTOQUISTA)).hasMessageContaining("ao menos um produto");
    }

    private UUID produtoComEstoque(int quantidade) {
        UUID id = UUID.randomUUID();
        Produto produto = new Produto("Arroz " + id, null, "10063021", "un", PRECO);
        produto.definirCusto(CUSTO);
        produto.darEntradaNoEstoque(quantidade);
        Produto espiao = spy(produto);
        when(espiao.id()).thenReturn(id);
        when(produtoRepository.buscarParaAlterarEstoque(id)).thenReturn(Optional.of(espiao));
        return id;
    }

    private MovimentacaoEstoque movimentacaoSalva() {
        ArgumentCaptor<MovimentacaoEstoque> salva = ArgumentCaptor.forClass(MovimentacaoEstoque.class);
        verify(movimentacaoRepository).save(salva.capture());
        return salva.getValue();
    }
}

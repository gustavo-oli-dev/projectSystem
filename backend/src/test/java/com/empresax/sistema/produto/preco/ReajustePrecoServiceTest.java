package com.empresax.sistema.produto.preco;

import com.empresax.sistema.common.domain.DomainException;
import com.empresax.sistema.produto.Produto;
import com.empresax.sistema.produto.ProdutoRepository;
import com.empresax.sistema.shared.dinheiro.Dinheiro;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReajustePrecoServiceTest {

    private static final String GERENTE = "gerente@x.com";

    private final ProdutoRepository produtoRepository = mock(ProdutoRepository.class);
    private final AlteracaoPrecoRepository alteracaoRepository = mock(AlteracaoPrecoRepository.class);
    private final ReajustePrecoService servico = new ReajustePrecoService(produtoRepository, alteracaoRepository);

    @Test
    void percentualReajustaCadaProdutoArredondandoNoCentavoERegistraOHistorico() {
        Produto arroz = produto("Arroz", "10.00");
        Produto feijao = produto("Feijão", "7.99");
        when(produtoRepository.findAllById(anyCollection())).thenReturn(List.of(arroz, feijao));

        List<ReajustePrecoService.PrecoAlterado> alterados = servico.reajustar(
                List.of(UUID.randomUUID(), UUID.randomUUID()), ModoReajuste.PERCENTUAL, new BigDecimal("5"), GERENTE);

        assertThat(arroz.precoUnitario()).isEqualTo(dinheiro("10.50"));
        assertThat(feijao.precoUnitario()).isEqualTo(dinheiro("8.39"));
        assertThat(alterados).hasSize(2);
        verify(alteracaoRepository, times(2)).save(any());
    }

    @Test
    void precoUnicoIgualAoAtualNaoGeraRegistro() {
        Produto refrigerante = produto("Refrigerante", "9.99");
        when(produtoRepository.findAllById(anyCollection())).thenReturn(List.of(refrigerante));

        List<ReajustePrecoService.PrecoAlterado> alterados = servico.reajustar(
                List.of(UUID.randomUUID()), ModoReajuste.PRECO_UNICO, new BigDecimal("9.99"), GERENTE);

        assertThat(alterados).isEmpty();
        verify(alteracaoRepository, times(0)).save(any());
    }

    @Test
    void reducaoDeCemPorCentoOuMaisEhRecusada() {
        when(produtoRepository.findAllById(anyCollection())).thenReturn(List.of(produto("Arroz", "10.00")));

        assertThatThrownBy(() -> servico.reajustar(List.of(UUID.randomUUID()), ModoReajuste.PERCENTUAL, new BigDecimal("-100"), GERENTE))
                .isInstanceOf(DomainException.class).hasMessageContaining("acima de −100%");
    }

    @Test
    void produtoQueNaoExisteMaisCancelaOLoteInteiro() {
        when(produtoRepository.findAllById(anyCollection())).thenReturn(List.of());

        assertThatThrownBy(() -> servico.reajustar(List.of(UUID.randomUUID()), ModoReajuste.PERCENTUAL, BigDecimal.TEN, GERENTE))
                .hasMessageContaining("não existe mais");
    }

    private static Produto produto(String nome, String preco) {
        Produto produto = new Produto(nome, null, "19059090", "UN", dinheiro(preco));
        ReflectionTestUtils.setField(produto, "id", UUID.randomUUID());
        return produto;
    }

    private static Dinheiro dinheiro(String valor) {
        return new Dinheiro(new BigDecimal(valor));
    }
}

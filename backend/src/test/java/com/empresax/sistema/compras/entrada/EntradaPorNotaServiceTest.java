package com.empresax.sistema.compras.entrada;

import com.empresax.sistema.compras.contas.ContaAPagarService;
import com.empresax.sistema.compras.contato.Contato;
import com.empresax.sistema.compras.contato.ContatoService;
import com.empresax.sistema.produto.Produto;
import com.empresax.sistema.produto.ProdutoService;
import com.empresax.sistema.produto.estoque.EstoqueService;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EntradaPorNotaServiceTest {

    private static final byte[] XML = LeitorXmlNfeTest.NOTA.getBytes(StandardCharsets.UTF_8);
    private static final String ESTOQUISTA = "estoque@x.com";
    private static final int ITEM_ARROZ = 1;
    private static final int ITEM_QUEIJO = 2;

    private final NotaEntradaRepository notaRepository = mock(NotaEntradaRepository.class);
    private final ContatoService contatoService = mock(ContatoService.class);
    private final ProdutoService produtoService = mock(ProdutoService.class);
    private final EstoqueService estoqueService = mock(EstoqueService.class);
    private final ContaAPagarService contaService = mock(ContaAPagarService.class);
    private final EntradaPorNotaService servico = new EntradaPorNotaService(
            new LeitorXmlNfe(), notaRepository, contatoService, produtoService, estoqueService, contaService);

    @Test
    void confirmarDaEntradaNoEstoqueAtualizaCustoELancaCadaParcela() {
        UUID fornecedor = fornecedor();
        UUID arroz = produto();
        when(notaRepository.saveAndFlush(any(NotaEntrada.class))).thenAnswer(chamada -> chamada.getArgument(0));

        NotaEntrada entrada = servico.confirmar(XML, Map.of(ITEM_ARROZ, arroz), true, ESTOQUISTA);

        assertThat(entrada.itens()).hasSize(1);
        verify(estoqueService).darEntradaPorNota(eq(arroz), eq(12), any(), any(), eq(ESTOQUISTA));
        verify(contaService, times(2)).lancarParcelaDaNota(eq(fornecedor), any(), anyString(), any(), any(LocalDate.class), eq(ESTOQUISTA));
    }

    @Test
    void semAtualizarCustoNaoMandaCustoNovo() {
        fornecedor();
        UUID arroz = produto();
        when(notaRepository.saveAndFlush(any(NotaEntrada.class))).thenAnswer(chamada -> chamada.getArgument(0));

        servico.confirmar(XML, Map.of(ITEM_ARROZ, arroz), false, ESTOQUISTA);

        verify(estoqueService).darEntradaPorNota(eq(arroz), eq(12), any(), isNull(), eq(ESTOQUISTA));
    }

    @Test
    void notaJaLancadaNaoEntraDeNovo() {
        NotaEntrada lancada = mock(NotaEntrada.class);
        when(lancada.registradaEm()).thenReturn(Instant.parse("2026-10-05T12:00:00Z"));
        when(lancada.registradaPor()).thenReturn("outro@x.com");
        when(notaRepository.findByChaveAcesso(anyString())).thenReturn(Optional.of(lancada));

        assertThatThrownBy(() -> servico.confirmar(XML, Map.of(ITEM_ARROZ, UUID.randomUUID()), false, ESTOQUISTA))
                .hasMessageContaining("já entrou no estoque");
        verify(estoqueService, never()).darEntradaPorNota(any(), any(Integer.class), any(), any(), any());
    }

    @Test
    void itemComQuantidadeFracionadaNaoEntraPelaNota() {
        fornecedor();
        UUID queijo = produto();

        assertThatThrownBy(() -> servico.confirmar(XML, Map.of(ITEM_QUEIJO, queijo), false, ESTOQUISTA))
                .hasMessageContaining("quantidade fracionada");
    }

    private UUID fornecedor() {
        UUID id = UUID.randomUUID();
        Contato contato = mock(Contato.class);
        when(contato.id()).thenReturn(id);
        when(contato.nome()).thenReturn("Distribuidora Exemplo Ltda");
        when(contatoService.fornecedorDaNota(anyString(), anyString(), any())).thenReturn(contato);
        when(notaRepository.findByChaveAcesso(anyString())).thenReturn(Optional.empty());
        return id;
    }

    private UUID produto() {
        UUID id = UUID.randomUUID();
        Produto produto = mock(Produto.class);
        when(produto.id()).thenReturn(id);
        when(produtoService.buscarPorId(id)).thenReturn(produto);
        return id;
    }
}

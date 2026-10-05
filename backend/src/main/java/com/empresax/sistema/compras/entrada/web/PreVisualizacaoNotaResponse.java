package com.empresax.sistema.compras.entrada.web;

import com.empresax.sistema.compras.contato.Contato;
import com.empresax.sistema.compras.entrada.EntradaPorNotaService.PreVisualizacao;
import com.empresax.sistema.compras.entrada.NotaDoFornecedor;
import com.empresax.sistema.compras.entrada.NotaEntrada;
import com.empresax.sistema.produto.Produto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** O que a nota trouxe, antes de entrar no estoque. */
public record PreVisualizacaoNotaResponse(
        String chaveAcesso,
        String numero,
        String serie,
        Instant emitidaEm,
        BigDecimal valorTotal,
        String fornecedorDocumento,
        String fornecedorNome,
        /** Vazio = o fornecedor será cadastrado ao confirmar. */
        UUID fornecedorCadastradoId,
        /** Preenchido = esta nota já entrou no estoque (não pode entrar de novo). */
        Instant jaLancadaEm,
        String jaLancadaPor,
        List<ItemResponse> itens,
        List<ParcelaResponse> parcelas
) {

    public record ItemResponse(
            int ordem, String codigo, String codigoBarras, String descricao, String unidade, BigDecimal quantidade,
            boolean quantidadeInteira, BigDecimal custoUnitario, UUID produtoSugeridoId, String produtoSugeridoNome
    ) {
    }

    public record ParcelaResponse(String numero, LocalDate vencimento, BigDecimal valor) {
    }

    static PreVisualizacaoNotaResponse de(PreVisualizacao previa) {
        NotaDoFornecedor nota = previa.nota();
        return new PreVisualizacaoNotaResponse(
                nota.chaveAcesso(), nota.numero(), nota.serie(), nota.emitidaEm(), nota.valorTotal(),
                nota.emitente().documento(), nota.emitente().nome(),
                previa.fornecedorCadastrado().map(Contato::id).orElse(null),
                previa.jaLancada().map(NotaEntrada::registradaEm).orElse(null),
                previa.jaLancada().map(NotaEntrada::registradaPor).orElse(null),
                nota.itens().stream().map(item -> {
                    Produto sugerido = previa.produtoSugeridoPorItem().get(item.ordem());
                    return new ItemResponse(item.ordem(), item.codigo(), item.codigoBarras(), item.descricao(), item.unidade(),
                            item.quantidade(), item.quantidadeInteira(), item.custoUnitario(),
                            sugerido == null ? null : sugerido.id(), sugerido == null ? null : sugerido.nome());
                }).toList(),
                nota.parcelas().stream()
                        .map(parcela -> new ParcelaResponse(parcela.numero(), parcela.vencimento(), parcela.valor()))
                        .toList());
    }
}

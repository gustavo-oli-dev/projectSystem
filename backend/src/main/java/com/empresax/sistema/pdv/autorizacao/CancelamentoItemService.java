package com.empresax.sistema.pdv.autorizacao;

import com.empresax.sistema.pdv.caixa.CaixaService;
import com.empresax.sistema.produto.Produto;
import com.empresax.sistema.produto.ProdutoService;
import com.empresax.sistema.relatorios.vendas.PeriodoRelatorio;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Cancelamento de item no caixa (D35): o item ainda não foi vendido (está no carrinho), então não
 * há estoque a devolver — o que importa é a trilha: qual caixa, qual operador, quem autorizou.
 */
@Service
public class CancelamentoItemService {

    private final ItemCanceladoCaixaRepository repository;
    private final AutorizacaoCaixaService autorizacaoService;
    private final CaixaService caixaService;
    private final ProdutoService produtoService;

    public CancelamentoItemService(
            ItemCanceladoCaixaRepository repository, AutorizacaoCaixaService autorizacaoService,
            CaixaService caixaService, ProdutoService produtoService
    ) {
        this.repository = repository;
        this.autorizacaoService = autorizacaoService;
        this.caixaService = caixaService;
        this.produtoService = produtoService;
    }

    @Transactional
    public ItemCanceladoCaixa registrar(UUID produtoId, int quantidade, String tokenAutorizacao, String operador) {
        String autorizadoPor = autorizacaoService.validar(tokenAutorizacao, AcaoAutorizada.CANCELAR_ITEM, operador);
        UUID caixa = caixaService.exigirCaixaAberto(operador).id();
        Produto produto = produtoService.buscarPorId(produtoId);
        return repository.save(new ItemCanceladoCaixa(
                caixa, produto.id(), produto.nome(), quantidade, produto.precoUnitario(), operador, autorizadoPor));
    }

    @Transactional(readOnly = true)
    public List<ItemCanceladoCaixa> doPeriodo(PeriodoRelatorio periodo) {
        return repository.findByCanceladoEmGreaterThanEqualAndCanceladoEmLessThanOrderByCanceladoEmDesc(
                periodo.comeco(), periodo.fimExclusivo());
    }
}

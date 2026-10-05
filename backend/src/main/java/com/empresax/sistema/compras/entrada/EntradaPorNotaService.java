package com.empresax.sistema.compras.entrada;

import com.empresax.sistema.common.domain.DomainException;
import com.empresax.sistema.compras.contas.ContaAPagarService;
import com.empresax.sistema.compras.contato.Contato;
import com.empresax.sistema.compras.contato.ContatoService;
import com.empresax.sistema.compras.entrada.NotaDoFornecedor.ItemDaNota;
import com.empresax.sistema.compras.entrada.NotaDoFornecedor.Parcela;
import com.empresax.sistema.produto.Produto;
import com.empresax.sistema.produto.ProdutoService;
import com.empresax.sistema.produto.estoque.EstoqueService;
import com.empresax.sistema.relatorios.vendas.PeriodoRelatorio;
import com.empresax.sistema.shared.dinheiro.Dinheiro;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Entrada de mercadoria pelo XML da nota do fornecedor (D34), em dois passos:
 * 1) pré-visualizar: lê o XML e mostra fornecedor, itens (com o produto sugerido pelo código de
 *    barras) e parcelas — nada é gravado;
 * 2) confirmar: o XML é lido de novo (nada vindo da tela é confiado além de "item X é o produto Y")
 *    e, numa transação só, cadastra o fornecedor se for novo, dá entrada no estoque, atualiza o custo
 *    (se pedido) e lança as parcelas em contas a pagar.
 */
@Service
public class EntradaPorNotaService {

    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy").withZone(PeriodoRelatorio.FUSO_DA_LOJA);

    private final LeitorXmlNfe leitor;
    private final NotaEntradaRepository notaRepository;
    private final ContatoService contatoService;
    private final ProdutoService produtoService;
    private final EstoqueService estoqueService;
    private final ContaAPagarService contaService;

    public EntradaPorNotaService(
            LeitorXmlNfe leitor, NotaEntradaRepository notaRepository, ContatoService contatoService,
            ProdutoService produtoService, EstoqueService estoqueService, ContaAPagarService contaService
    ) {
        this.leitor = leitor;
        this.notaRepository = notaRepository;
        this.contatoService = contatoService;
        this.produtoService = produtoService;
        this.estoqueService = estoqueService;
        this.contaService = contaService;
    }

    @Transactional(readOnly = true)
    public PreVisualizacao preVisualizar(byte[] xml) {
        NotaDoFornecedor nota = leitor.ler(xml);
        Map<Integer, Produto> sugeridos = nota.itens().stream()
                .flatMap(item -> produtoService.encontrarPorCodigoBarras(item.codigoBarras()).map(produto -> Map.entry(item.ordem(), produto)).stream())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        return new PreVisualizacao(
                nota,
                contatoService.buscarPorDocumento(nota.emitente().documento()),
                notaRepository.findByChaveAcesso(nota.chaveAcesso()),
                sugeridos);
    }

    /**
     * @param produtoPorItem ordem do item na nota → produto do sistema. Item fora do mapa é ignorado
     *                       (não entra no estoque).
     */
    @Transactional
    public NotaEntrada confirmar(byte[] xml, Map<Integer, UUID> produtoPorItem, boolean atualizarCusto, String responsavel) {
        NotaDoFornecedor nota = leitor.ler(xml);
        notaRepository.findByChaveAcesso(nota.chaveAcesso()).ifPresent(jaLancada -> {
            throw new DomainException("Esta nota já entrou no estoque em " + DATA.format(jaLancada.registradaEm())
                    + " (lançada por " + jaLancada.registradaPor() + ")");
        });
        Contato fornecedor = contatoService.fornecedorDaNota(
                nota.emitente().documento(), nota.emitente().nome(), nota.emitente().telefone());
        Map<Integer, ItemDaNota> itensPorOrdem = nota.itens().stream().collect(Collectors.toMap(ItemDaNota::ordem, Function.identity()));

        List<NotaEntrada.Item> ligados = produtoPorItem.entrySet().stream()
                .map(ligacao -> ligar(itensPorOrdem.get(ligacao.getKey()), ligacao.getKey(), ligacao.getValue()))
                .toList();
        NotaEntrada entrada = salvar(new NotaEntrada(nota, fornecedor.id(), ligados, responsavel));

        entrada.itens().forEach(item -> estoqueService.darEntradaPorNota(
                item.produtoId(), item.quantidade(), entrada.id(),
                atualizarCusto ? new Dinheiro(item.custoUnitario()) : null, responsavel));
        lancarParcelas(nota, fornecedor, entrada, responsavel);
        return entrada;
    }

    private NotaEntrada.Item ligar(ItemDaNota item, int ordem, UUID produtoId) {
        if (item == null) {
            throw new DomainException("A nota não tem o item " + ordem);
        }
        if (!item.quantidadeInteira()) {
            throw new DomainException("O item \"" + item.descricao() + "\" veio com quantidade fracionada ("
                    + item.quantidade().stripTrailingZeros().toPlainString() + " " + item.unidade()
                    + "); o estoque é em unidades inteiras — dê entrada desse item à mão");
        }
        Produto produto = produtoService.buscarPorId(produtoId);
        return new NotaEntrada.Item(ordem, produto.id(), item.descricao(), item.quantidade().intValueExact(), item.custoUnitario());
    }

    /** Duplicatas viram contas a pagar; nota sem duplicata vira uma conta só, com vencimento na emissão. */
    private void lancarParcelas(NotaDoFornecedor nota, Contato fornecedor, NotaEntrada entrada, String responsavel) {
        String prefixo = "NF-e nº " + nota.numero() + " — " + fornecedor.nome();
        if (nota.parcelas().isEmpty()) {
            contaService.lancarParcelaDaNota(fornecedor.id(), entrada.id(), prefixo + " (sem parcelas na nota)",
                    new Dinheiro(nota.valorTotal()), nota.emitidaEm().atZone(PeriodoRelatorio.FUSO_DA_LOJA).toLocalDate(), responsavel);
            return;
        }
        for (Parcela parcela : nota.parcelas()) {
            contaService.lancarParcelaDaNota(fornecedor.id(), entrada.id(), prefixo + " — parcela " + parcela.numero(),
                    new Dinheiro(parcela.valor()), parcela.vencimento(), responsavel);
        }
    }

    /** Duas pessoas lançando a mesma nota juntas: o índice único da chave barra a segunda. */
    private NotaEntrada salvar(NotaEntrada entrada) {
        try {
            return notaRepository.saveAndFlush(entrada);
        } catch (DataIntegrityViolationException mesmaNotaAoMesmoTempo) {
            throw new DomainException("Esta nota já entrou no estoque");
        }
    }

    @Transactional(readOnly = true)
    public List<NotaEntrada> ultimasNotas() {
        List<NotaEntrada> notas = notaRepository.findTop50ByOrderByRegistradaEmDesc();
        notas.forEach(nota -> nota.itens().size());
        return notas;
    }

    /** O que a tela mostra antes de confirmar. */
    public record PreVisualizacao(
            NotaDoFornecedor nota,
            Optional<Contato> fornecedorCadastrado,
            Optional<NotaEntrada> jaLancada,
            Map<Integer, Produto> produtoSugeridoPorItem
    ) {
    }
}

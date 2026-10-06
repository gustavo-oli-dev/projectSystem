package com.empresax.sistema.promocao;

import com.empresax.sistema.common.domain.DomainException;
import com.empresax.sistema.common.domain.EntidadeNaoEncontradaException;
import com.empresax.sistema.produto.Produto;
import com.empresax.sistema.produto.ProdutoService;
import com.empresax.sistema.shared.dinheiro.Dinheiro;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Cadastro das promoções e o desconto que cada venda ganha por elas (D38). */
@Service
public class PromocaoService {

    private final PromocaoRepository promocaoRepository;
    private final ProdutoService produtoService;
    private final Clock relogioDaLoja;

    public PromocaoService(PromocaoRepository promocaoRepository, ProdutoService produtoService, Clock relogioDaLoja) {
        this.promocaoRepository = promocaoRepository;
        this.produtoService = produtoService;
        this.relogioDaLoja = relogioDaLoja;
    }

    @Transactional
    public Promocao criarPrecoDeOferta(UUID produtoId, Dinheiro precoOferta, LocalDate inicio, LocalDate fim, String quem) {
        Produto produto = produtoService.buscarPorId(produtoId);
        return salvarSemCoincidir(Promocao.precoDeOferta(produto.id(), precoOferta, produto.precoUnitario(), inicio, fim, quem));
    }

    @Transactional
    public Promocao criarLevePague(UUID produtoId, int leve, int pague, LocalDate inicio, LocalDate fim, String quem) {
        Produto produto = produtoService.buscarPorId(produtoId);
        return salvarSemCoincidir(Promocao.levePague(produto.id(), leve, pague, inicio, fim, quem));
    }

    private Promocao salvarSemCoincidir(Promocao nova) {
        if (nova.inicio().isBefore(hoje())) {
            throw new DomainException("A promoção não pode começar num dia que já passou");
        }
        boolean coincide = promocaoRepository.findByProdutoId(nova.produtoId()).stream()
                .anyMatch(existente -> existente.coincideCom(nova.inicio(), nova.fim()));
        if (coincide) {
            throw new DomainException("Este produto já tem promoção nesses dias: encerre a outra ou escolha outras datas");
        }
        return promocaoRepository.save(nova);
    }

    @Transactional
    public Promocao encerrar(UUID promocaoId, String quem) {
        Promocao promocao = promocaoRepository.findById(promocaoId)
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Promoção não encontrada: " + promocaoId));
        promocao.encerrar(quem, hoje());
        return promocao;
    }

    @Transactional(readOnly = true)
    public List<Promocao> listar() {
        return promocaoRepository.findAllByOrderByInicioDesc();
    }

    /** As que valem hoje — o caixa usa para mostrar o preço certo antes de fechar a venda. */
    @Transactional(readOnly = true)
    public List<Promocao> valendoHoje() {
        return promocaoRepository.todasValendoNoDia(hoje());
    }

    /** Promoção de cada produto pedido que vale hoje (no máximo uma por produto). */
    @Transactional(readOnly = true)
    public Map<UUID, Promocao> valendoHojePara(Collection<UUID> produtos) {
        if (produtos.isEmpty()) {
            return Map.of();
        }
        return promocaoRepository.valendoNoDia(produtos, hoje()).stream()
                .collect(Collectors.toMap(Promocao::produtoId, Function.identity(), (uma, outra) -> uma));
    }

    @Transactional(readOnly = true)
    public Optional<Promocao> valendoHojePara(UUID produtoId) {
        return Optional.ofNullable(valendoHojePara(List.of(produtoId)).get(produtoId));
    }

    public LocalDate hoje() {
        return LocalDate.now(relogioDaLoja);
    }
}

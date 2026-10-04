package com.empresax.sistema.pdv.caixa;

import com.empresax.sistema.common.domain.DomainException;
import com.empresax.sistema.common.domain.EntidadeNaoEncontradaException;
import com.empresax.sistema.pdv.caixa.VendasDoCaixaConsulta.VendasPorForma;
import com.empresax.sistema.shared.dinheiro.Dinheiro;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Abertura, reposição de troco, sangria e fechamento do caixa de cada operador (D26). Cada operador
 * mexe só no próprio caixa; a conferência de todos fica com quem tem CAIXA_CONFERIR.
 */
@Service
public class CaixaService {

    private static final String MENSAGEM_JA_ABERTO = "Você já tem um caixa aberto — feche-o antes de abrir outro";
    private static final String MOTIVO_DEVOLUCAO = "Devolução em dinheiro da venda %s (cancelada depois do fechamento do caixa dela)";

    private final SessaoCaixaRepository sessaoRepository;
    private final CedulaFundoTrocoPadraoRepository fundoPadraoRepository;
    private final VendasDoCaixaConsulta vendasDoCaixa;

    public CaixaService(
            SessaoCaixaRepository sessaoRepository,
            CedulaFundoTrocoPadraoRepository fundoPadraoRepository,
            VendasDoCaixaConsulta vendasDoCaixa
    ) {
        this.sessaoRepository = sessaoRepository;
        this.fundoPadraoRepository = fundoPadraoRepository;
        this.vendasDoCaixa = vendasDoCaixa;
    }

    @Transactional(readOnly = true)
    public Optional<SessaoCaixa> caixaAberto(String operador) {
        return sessaoRepository.findByOperadorAndStatus(operador, StatusSessaoCaixa.ABERTA).map(CaixaService::carregada);
    }

    /** Usado pela venda: não existe venda de balcão fora de um caixa aberto. */
    @Transactional(readOnly = true)
    public SessaoCaixa exigirCaixaAberto(String operador) {
        return caixaAberto(operador).orElseThrow(() -> new DomainException("Abra o caixa antes de vender"));
    }

    @Transactional
    public SessaoCaixa abrir(String operador, ContagemCedulas fundoDeTroco) {
        if (caixaAberto(operador).isPresent()) {
            throw new DomainException(MENSAGEM_JA_ABERTO);
        }
        try {
            return carregada(sessaoRepository.saveAndFlush(SessaoCaixa.abrir(operador, fundoDeTroco)));
        } catch (DataIntegrityViolationException duasAberturasAoMesmoTempo) {
            // Duas abas abrindo juntas: o índice único do banco barra a segunda.
            throw new DomainException(MENSAGEM_JA_ABERTO);
        }
    }

    @Transactional
    public SessaoCaixa registrarSuprimento(String operador, ContagemCedulas cedulas, String motivo) {
        SessaoCaixa sessao = buscarAbertoParaAlterar(operador);
        sessao.registrarSuprimento(cedulas, motivo, operador);
        return carregada(sessao);
    }

    @Transactional
    public SessaoCaixa registrarSangria(String operador, Dinheiro valor, String motivo) {
        SessaoCaixa sessao = buscarAbertoParaAlterar(operador);
        sessao.registrarSangria(valor, motivo, operador, vendasDoCaixa.emDinheiro(sessao.id()));
        return carregada(sessao);
    }

    @Transactional
    public FechamentoCaixa fechar(String operador, ContagemCedulas contagem, String observacao) {
        SessaoCaixa sessao = buscarAbertoParaAlterar(operador);
        sessao.fechar(contagem, vendasDoCaixa.emDinheiro(sessao.id()), observacao);
        return new FechamentoCaixa(carregada(sessao), vendasDoCaixa.porForma(sessao.id()));
    }

    /**
     * Cancelamento em dinheiro: com o caixa da venda ainda aberto, a venda sai da conta dele
     * sozinha. Com ele já fechado, o dinheiro sai da gaveta de quem está cancelando — vira uma
     * sangria no caixa dessa pessoa, para a conferência dela bater.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void registrarDevolucaoEmDinheiro(UUID caixaDaVenda, Dinheiro valor, UUID pedidoId, String quemCancela) {
        boolean caixaDaVendaAberto = sessaoRepository.findById(caixaDaVenda).map(SessaoCaixa::aberta).orElse(false);
        if (caixaDaVendaAberto) {
            return;
        }
        SessaoCaixa caixaDeQuemCancela = sessaoRepository.buscarAbertaParaAlterar(quemCancela)
                .orElseThrow(() -> new DomainException(
                        "O caixa desta venda já foi fechado: abra o seu caixa para devolver o dinheiro ao cliente"));
        caixaDeQuemCancela.registrarSangria(valor, MOTIVO_DEVOLUCAO.formatted(pedidoId), quemCancela,
                vendasDoCaixa.emDinheiro(caixaDeQuemCancela.id()));
    }

    @Transactional(readOnly = true)
    public List<ResumoSessaoCaixa> listarParaConferencia() {
        List<SessaoCaixa> sessoes = sessaoRepository.findTop100ByOrderByAbertaEmDesc();
        Map<UUID, Dinheiro> dinheiroPorSessao = vendasDoCaixa.emDinheiro(
                sessoes.stream().map(SessaoCaixa::id).collect(Collectors.toSet()));
        return sessoes.stream()
                .map(sessao -> new ResumoSessaoCaixa(sessao, dinheiroPorSessao.getOrDefault(sessao.id(), Dinheiro.zero())))
                .toList();
    }

    @Transactional(readOnly = true)
    public FechamentoCaixa detalharParaConferencia(UUID sessaoId) {
        SessaoCaixa sessao = sessaoRepository.findById(sessaoId)
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Caixa não encontrado: " + sessaoId));
        return new FechamentoCaixa(carregada(sessao), vendasDoCaixa.porForma(sessaoId));
    }

    @Transactional(readOnly = true)
    public ContagemCedulas fundoDeTrocoPadrao() {
        return new ContagemCedulas(fundoPadraoRepository.findAll().stream()
                .collect(Collectors.toMap(CedulaFundoTrocoPadrao::cedula, CedulaFundoTrocoPadrao::quantidade)));
    }

    @Transactional
    public ContagemCedulas definirFundoDeTrocoPadrao(ContagemCedulas composicao) {
        fundoPadraoRepository.deleteAllInBatch();
        fundoPadraoRepository.saveAll(composicao.quantidades().entrySet().stream()
                .map(entrada -> new CedulaFundoTrocoPadrao(entrada.getKey(), entrada.getValue()))
                .toList());
        return composicao;
    }

    /** A resposta é montada fora da transação (open-in-view desligado): carrega as coleções antes. */
    private static SessaoCaixa carregada(SessaoCaixa sessao) {
        sessao.movimentos().forEach(MovimentoCaixa::cedulas);
        sessao.cedulasAbertura();
        sessao.cedulasFechamento();
        return sessao;
    }

    private SessaoCaixa buscarAbertoParaAlterar(String operador) {
        return sessaoRepository.buscarAbertaParaAlterar(operador)
                .orElseThrow(() -> new DomainException("Você não tem um caixa aberto"));
    }

    /** Caixa + quanto foi vendido em cada forma de pagamento. */
    public record FechamentoCaixa(SessaoCaixa sessao, List<VendasPorForma> vendasPorForma) {
        public FechamentoCaixa {
            vendasPorForma = List.copyOf(vendasPorForma);
        }
    }

    /** Linha da lista de conferência: caixa aberto ainda não tem contagem, mas já tem as vendas em dinheiro. */
    public record ResumoSessaoCaixa(SessaoCaixa sessao, Dinheiro vendasEmDinheiroAteAgora) {
    }
}

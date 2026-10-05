package com.empresax.sistema.pdv.caixa;

import com.empresax.sistema.common.domain.DomainException;
import com.empresax.sistema.common.domain.EntidadeNaoEncontradaException;
import com.empresax.sistema.pdv.FormaPagamentoPresencial;
import com.empresax.sistema.pdv.caixa.VendasDoCaixaConsulta.VendasPorForma;
import com.empresax.sistema.shared.dinheiro.Dinheiro;
import com.empresax.sistema.usuario.Usuario;
import com.empresax.sistema.usuario.UsuarioService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Gestão de caixa separada da venda (D26/D27). O caixa pertence ao operador que vende; abrir,
 * repor troco, fazer sangria e fechar são feitos por quem tem CAIXA_GERENCIAR, em qualquer caixa.
 * A conferência de todos fica com quem tem CAIXA_CONFERIR.
 */
@Service
public class CaixaService {

    private static final String MENSAGEM_OPERADOR_JA_ABERTO = "%s já está em um caixa aberto — feche-o antes de abrir outro";
    private static final String MENSAGEM_CAIXA_JA_ABERTO = "%s já está aberto — feche-o antes de abrir de novo";
    private static final String MOTIVO_DEVOLUCAO = "Devolução em dinheiro da venda %s (cancelada depois do fechamento do caixa dela)";

    private final SessaoCaixaRepository sessaoRepository;
    private final PontoCaixaRepository pontoRepository;
    private final CedulaFundoTrocoPadraoRepository fundoPadraoRepository;
    private final VendasDoCaixaConsulta vendasDoCaixa;
    private final UsuarioService usuarioService;

    public CaixaService(
            SessaoCaixaRepository sessaoRepository,
            PontoCaixaRepository pontoRepository,
            CedulaFundoTrocoPadraoRepository fundoPadraoRepository,
            VendasDoCaixaConsulta vendasDoCaixa,
            UsuarioService usuarioService
    ) {
        this.sessaoRepository = sessaoRepository;
        this.pontoRepository = pontoRepository;
        this.fundoPadraoRepository = fundoPadraoRepository;
        this.vendasDoCaixa = vendasDoCaixa;
        this.usuarioService = usuarioService;
    }

    /** O caixa aberto do operador (a tela de venda mostra se pode vender). */
    @Transactional(readOnly = true)
    public Optional<SessaoCaixa> caixaAberto(String operador) {
        return sessaoRepository.findByOperadorAndStatus(operador, StatusSessaoCaixa.ABERTA).map(CaixaService::carregada);
    }

    /** Usado pela venda: não existe venda de balcão fora de um caixa aberto. */
    @Transactional(readOnly = true)
    public SessaoCaixa exigirCaixaAberto(String operador) {
        return caixaAberto(operador).orElseThrow(() -> new DomainException(
                "Seu caixa ainda não foi aberto — peça a um responsável pelo caixa para abrir"));
    }

    /**
     * Abre os caixas escolhidos (Caixa 01, 02...), cada um com o seu operador e o mesmo fundo de
     * troco (uma gaveta para cada). Tudo ou nada: se um não puder abrir, nenhum é aberto.
     */
    @Transactional
    public List<SessaoCaixa> abrir(List<AberturaDeCaixa> aberturas, ContagemCedulas fundoDeTroco, String abertaPor) {
        if (aberturas == null || aberturas.isEmpty()) {
            throw new DomainException("Escolha ao menos um caixa para abrir");
        }
        if (aberturas.stream().map(AberturaDeCaixa::pontoCaixaId).distinct().count() != aberturas.size()) {
            throw new DomainException("O mesmo caixa foi escolhido mais de uma vez");
        }
        if (aberturas.stream().map(AberturaDeCaixa::operadorId).distinct().count() != aberturas.size()) {
            throw new DomainException("O mesmo operador foi escolhido para mais de um caixa");
        }
        return aberturas.stream().map(abertura -> abrirUm(abertura, fundoDeTroco, abertaPor)).toList();
    }

    private SessaoCaixa abrirUm(AberturaDeCaixa abertura, ContagemCedulas fundoDeTroco, String abertaPor) {
        PontoCaixa ponto = pontoRepository.findById(abertura.pontoCaixaId())
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Caixa não encontrado: " + abertura.pontoCaixaId()));
        if (!ponto.ativo()) {
            throw new DomainException(ponto.nome() + " está desativado");
        }
        if (sessaoRepository.findByPontoCaixaIdAndStatus(ponto.id(), StatusSessaoCaixa.ABERTA).isPresent()) {
            throw new DomainException(MENSAGEM_CAIXA_JA_ABERTO.formatted(ponto.nome()));
        }
        Usuario operador = usuarioService.buscarOperadorDeCaixa(abertura.operadorId());
        if (caixaAberto(operador.email()).isPresent()) {
            throw new DomainException(MENSAGEM_OPERADOR_JA_ABERTO.formatted(operador.nome()));
        }
        try {
            return carregada(sessaoRepository.saveAndFlush(
                    SessaoCaixa.abrir(operador.email(), ponto.id(), fundoDeTroco, abertaPor)));
        } catch (DataIntegrityViolationException duasAberturasAoMesmoTempo) {
            // Duas pessoas abrindo o mesmo caixa ou operador juntas: o índice único do banco barra a segunda.
            throw new DomainException(MENSAGEM_CAIXA_JA_ABERTO.formatted(ponto.nome()));
        }
    }

    @Transactional(readOnly = true)
    public List<PontoCaixa> listarPontos() {
        return pontoRepository.findAllByOrderByNumeroAsc();
    }

    @Transactional
    public PontoCaixa cadastrarPonto(int numero) {
        if (pontoRepository.existsByNumero(numero)) {
            throw new DomainException("Já existe o caixa número " + numero);
        }
        return pontoRepository.save(new PontoCaixa(numero));
    }

    @Transactional
    public PontoCaixa definirPontoAtivo(UUID pontoId, boolean ativo) {
        PontoCaixa ponto = pontoRepository.findById(pontoId)
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Caixa não encontrado: " + pontoId));
        if (ativo) {
            ponto.ativar();
        } else {
            ponto.desativar();
        }
        return ponto;
    }

    /** Ids dos caixas físicos que estão abertos agora (a tela de abertura só oferece os livres). */
    @Transactional(readOnly = true)
    public Set<UUID> pontosAbertos() {
        return listarAbertos().stream().flatMap(sessao -> sessao.pontoCaixaId().stream()).collect(Collectors.toSet());
    }

    @Transactional
    public SessaoCaixa registrarSuprimento(UUID sessaoId, ContagemCedulas cedulas, String motivo, String registradoPor) {
        SessaoCaixa sessao = buscarParaAlterar(sessaoId);
        sessao.registrarSuprimento(cedulas, motivo, registradoPor);
        return carregada(sessao);
    }

    @Transactional
    public SessaoCaixa registrarSangria(UUID sessaoId, Dinheiro valor, String motivo, String registradoPor) {
        SessaoCaixa sessao = buscarParaAlterar(sessaoId);
        sessao.registrarSangria(valor, motivo, registradoPor, vendasDoCaixa.emDinheiro(sessaoId));
        return carregada(sessao);
    }

    @Transactional
    public FechamentoCaixa fechar(
            UUID sessaoId, ContagemCedulas contagem, Map<FormaPagamentoPresencial, Dinheiro> informadoDaMaquininha,
            String observacao, String fechadaPor
    ) {
        SessaoCaixa sessao = buscarParaAlterar(sessaoId);
        List<VendasPorForma> porForma = vendasDoCaixa.porForma(sessaoId);
        ConferenciaMaquininha maquininha = ConferenciaMaquininha.de(registradoPorForma(porForma), informadoDaMaquininha);
        sessao.fechar(contagem, vendasDoCaixa.emDinheiro(sessaoId), maquininha, observacao, fechadaPor);
        return new FechamentoCaixa(carregada(sessao), porForma);
    }

    /** Vendas por forma da consulta → só as formas presenciais (Pix por QR é confirmado pelo Mercado Pago). */
    private static Map<FormaPagamentoPresencial, Dinheiro> registradoPorForma(List<VendasPorForma> porForma) {
        Set<String> presenciais = Arrays.stream(FormaPagamentoPresencial.values()).map(Enum::name).collect(Collectors.toSet());
        return porForma.stream()
                .filter(forma -> presenciais.contains(forma.forma()))
                .collect(Collectors.toMap(forma -> FormaPagamentoPresencial.valueOf(forma.forma()), VendasPorForma::valor));
    }

    /**
     * Cancelamento em dinheiro: com o caixa da venda ainda aberto, a venda sai da conta dele
     * sozinha. Com ele já fechado, o dinheiro sai da gaveta do caixa aberto de quem está
     * cancelando — vira uma sangria nele, para a conferência bater.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void registrarDevolucaoEmDinheiro(UUID caixaDaVenda, Dinheiro valor, UUID pedidoId, String quemCancela) {
        boolean caixaDaVendaAberto = sessaoRepository.findById(caixaDaVenda).map(SessaoCaixa::aberta).orElse(false);
        if (caixaDaVendaAberto) {
            return;
        }
        SessaoCaixa caixaDeQuemCancela = sessaoRepository.buscarAbertaDoOperadorParaAlterar(quemCancela)
                .orElseThrow(() -> new DomainException(
                        "O caixa desta venda já foi fechado: cancele a partir de um caixa aberto (o dinheiro sai da gaveta dele)"));
        caixaDeQuemCancela.registrarSangria(valor, MOTIVO_DEVOLUCAO.formatted(pedidoId), quemCancela,
                vendasDoCaixa.emDinheiro(caixaDeQuemCancela.id()));
    }

    @Transactional(readOnly = true)
    public List<SessaoCaixa> listarAbertos() {
        return sessaoRepository.findByStatusOrderByAbertaEmAsc(StatusSessaoCaixa.ABERTA).stream()
                .map(CaixaService::carregada)
                .toList();
    }

    /** Quem pode vender e se já está com caixa aberto (a tela de abertura só oferece quem não está). */
    @Transactional(readOnly = true)
    public List<OperadorDeCaixa> operadores() {
        Set<String> comCaixaAberto = listarAbertos().stream().map(SessaoCaixa::operador).collect(Collectors.toSet());
        return usuarioService.listarQuemPodeVender().stream()
                .map(usuario -> new OperadorDeCaixa(usuario.id(), usuario.nome(), usuario.email(), comCaixaAberto.contains(usuario.email())))
                .toList();
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
        sessao.conferenciasForma().size();
        return sessao;
    }

    private SessaoCaixa buscarParaAlterar(UUID sessaoId) {
        return sessaoRepository.buscarParaAlterar(sessaoId)
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Caixa não encontrado: " + sessaoId));
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

    /** Qual caixa físico abre e quem vai operar nele. */
    public record AberturaDeCaixa(UUID pontoCaixaId, UUID operadorId) {
    }

    public record OperadorDeCaixa(UUID id, String nome, String email, boolean caixaAberto) {
    }
}

package com.empresax.sistema.pdv.caixa;

import com.empresax.sistema.common.domain.DomainException;
import com.empresax.sistema.shared.dinheiro.Dinheiro;
import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.MapKeyEnumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Um turno de caixa de um operador: abre com o fundo de troco contado por cédula, recebe reposições
 * de troco (suprimento) e retiradas (sangria) e fecha com a contagem da gaveta. Abrir, repor,
 * sangrar e fechar são feitos por quem tem CAIXA_GERENCIAR (D27); o operador só vende.
 *
 * Dinheiro que deveria estar na gaveta = fundo inicial + vendas em dinheiro + suprimentos − sangrias.
 * O que entrou no dia = contado − fundo inicial − suprimentos + sangrias. A diferença (sobra ou
 * falta) é contado − esperado, congelada no fechamento.
 *
 * As vendas em dinheiro não ficam aqui (são pagamentos de pedidos): o serviço as soma e entrega.
 */
@Entity
@Table(name = "sessoes_caixa")
public class SessaoCaixa {

    private static final int TAMANHO_MAXIMO_OBSERVACAO = 500;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, updatable = false)
    private String operador;

    /** Caixa físico (Caixa 01...). Vazio só nas sessões anteriores aos caixas numerados. */
    @Column(updatable = false)
    private UUID pontoCaixaId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusSessaoCaixa status;

    /** Quem abriu (tem CAIXA_GERENCIAR); o caixa é do operador, que só vende. */
    @Column(nullable = false, updatable = false)
    private String abertaPor;

    @Column(nullable = false, updatable = false)
    private Instant abertaEm;

    @Column(nullable = false, updatable = false)
    private Dinheiro fundoInicial;

    @ElementCollection
    @CollectionTable(name = "sessoes_caixa_cedulas_abertura", joinColumns = @JoinColumn(name = "sessao_caixa_id"))
    @MapKeyEnumerated(EnumType.STRING)
    @MapKeyColumn(name = "cedula")
    @Column(name = "quantidade")
    private Map<Cedula, Integer> cedulasAbertura = new HashMap<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "sessao_caixa_id", nullable = false, updatable = false)
    @OrderBy("registradoEm ASC")
    private List<MovimentoCaixa> movimentos = new ArrayList<>();

    @Column
    private Instant fechadaEm;

    @Column
    private String fechadaPor;

    @ElementCollection
    @CollectionTable(name = "sessoes_caixa_cedulas_fechamento", joinColumns = @JoinColumn(name = "sessao_caixa_id"))
    @MapKeyEnumerated(EnumType.STRING)
    @MapKeyColumn(name = "cedula")
    @Column(name = "quantidade")
    private Map<Cedula, Integer> cedulasFechamento = new HashMap<>();

    @ElementCollection
    @CollectionTable(name = "conferencias_forma_caixa", joinColumns = @JoinColumn(name = "sessao_caixa_id"))
    private List<ConferenciaForma> conferenciasForma = new ArrayList<>();

    @Column
    private Dinheiro valorContado;

    /** Congelado no fechamento: um cancelamento depois não reescreve a conferência já feita. */
    @Column
    private Dinheiro vendasEmDinheiro;

    /** Pode ser negativo só em caso extremo (venda cancelada depois de uma sangria). */
    @Column
    private BigDecimal valorEsperado;

    @Column(length = TAMANHO_MAXIMO_OBSERVACAO)
    private String observacaoFechamento;

    protected SessaoCaixa() {
        // exigido pelo JPA
    }

    private SessaoCaixa(String operador, UUID pontoCaixaId, ContagemCedulas fundo, String abertaPor) {
        if (operador == null || operador.isBlank()) {
            throw new DomainException("Caixa precisa de um operador");
        }
        if (pontoCaixaId == null) {
            throw new DomainException("Escolha qual caixa (Caixa 01, 02...) está sendo aberto");
        }
        if (abertaPor == null || abertaPor.isBlank()) {
            throw new DomainException("Informe quem está abrindo o caixa");
        }
        if (fundo == null) {
            throw new DomainException("Informe as cédulas do fundo de troco");
        }
        this.operador = operador;
        this.pontoCaixaId = pontoCaixaId;
        this.abertaPor = abertaPor;
        this.status = StatusSessaoCaixa.ABERTA;
        this.abertaEm = Instant.now();
        this.fundoInicial = fundo.total();
        this.cedulasAbertura = new HashMap<>(fundo.quantidades());
    }

    /** Abre o caixa físico {@code pontoCaixaId} para o operador, com o fundo de troco contado. */
    public static SessaoCaixa abrir(String operador, UUID pontoCaixaId, ContagemCedulas fundoDeTroco, String abertaPor) {
        return new SessaoCaixa(operador, pontoCaixaId, fundoDeTroco, abertaPor);
    }

    /** Reposição de troco: o gerente traz notas trocadas para a gaveta. */
    public MovimentoCaixa registrarSuprimento(ContagemCedulas cedulas, String motivo, String registradoPor) {
        garantirAberta();
        if (cedulas == null || cedulas.estaVazia()) {
            throw new DomainException("Informe as cédulas da reposição de troco");
        }
        MovimentoCaixa suprimento = MovimentoCaixa.suprimento(cedulas, motivo, registradoPor);
        movimentos.add(suprimento);
        return suprimento;
    }

    /** Retirada da gaveta. Não pode tirar mais do que deveria haver nela. */
    public MovimentoCaixa registrarSangria(Dinheiro valor, String motivo, String registradoPor, Dinheiro vendasEmDinheiroAteAgora) {
        garantirAberta();
        if (valor != null && valor.valor().compareTo(dinheiroEsperado(vendasEmDinheiroAteAgora)) > 0) {
            throw new DomainException("A sangria é maior do que o dinheiro que deveria estar no caixa");
        }
        MovimentoCaixa sangria = MovimentoCaixa.sangria(valor, motivo, registradoPor);
        movimentos.add(sangria);
        return sangria;
    }

    /**
     * Fechamento cego: quem fecha conta a gaveta e digita o que o relatório da maquininha mostra
     * (crédito, débito, Pix) sem ver o que o sistema registrou; o sistema compara depois. Diferença
     * não impede o fechamento — fica registrada para a conferência.
     */
    public void fechar(ContagemCedulas contagem, Dinheiro vendasEmDinheiroDoTurno, ConferenciaMaquininha maquininha,
                       String observacao, String fechadaPor) {
        garantirAberta();
        if (maquininha == null) {
            throw new DomainException("Informe os valores do relatório da maquininha");
        }
        if (fechadaPor == null || fechadaPor.isBlank()) {
            throw new DomainException("Informe quem está fechando o caixa");
        }
        if (contagem == null) {
            throw new DomainException("Informe a contagem da gaveta");
        }
        if (vendasEmDinheiroDoTurno == null) {
            throw new DomainException("Vendas em dinheiro do turno são obrigatórias no fechamento");
        }
        if (observacao != null && observacao.trim().length() > TAMANHO_MAXIMO_OBSERVACAO) {
            throw new DomainException("Observação muito longa (máximo de " + TAMANHO_MAXIMO_OBSERVACAO + " caracteres)");
        }
        this.conferenciasForma = new ArrayList<>(maquininha.conferencias());
        this.cedulasFechamento = new HashMap<>(contagem.quantidades());
        this.valorContado = contagem.total();
        this.vendasEmDinheiro = vendasEmDinheiroDoTurno;
        this.valorEsperado = dinheiroEsperado(vendasEmDinheiroDoTurno);
        this.observacaoFechamento = observacao == null || observacao.isBlank() ? null : observacao.trim();
        this.fechadaPor = fechadaPor;
        this.fechadaEm = Instant.now();
        this.status = StatusSessaoCaixa.FECHADA;
    }

    public BigDecimal dinheiroEsperado(Dinheiro vendasEmDinheiroAteAgora) {
        return fundoInicial.valor()
                .add(vendasEmDinheiroAteAgora.valor())
                .add(efeitoDosMovimentos());
    }

    /** Sobra (positivo) ou falta (negativo). Só existe depois do fechamento. */
    public Optional<BigDecimal> diferenca() {
        return Optional.ofNullable(valorContado).map(contado -> contado.valor().subtract(valorEsperado));
    }

    /** "O quanto entrou menos o valor inicial do dia", descontando as reposições e somando o que saiu em sangria. */
    public Optional<BigDecimal> dinheiroQueEntrou() {
        return Optional.ofNullable(valorContado)
                .map(contado -> contado.valor().subtract(fundoInicial.valor()).subtract(efeitoDosMovimentos()));
    }

    public Dinheiro totalSuprimentos() {
        return somar(TipoMovimentoCaixa.SUPRIMENTO);
    }

    public Dinheiro totalSangrias() {
        return somar(TipoMovimentoCaixa.SANGRIA);
    }

    public boolean aberta() {
        return status == StatusSessaoCaixa.ABERTA;
    }

    public boolean doOperador(String email) {
        return operador.equals(email);
    }

    private BigDecimal efeitoDosMovimentos() {
        return movimentos.stream().map(MovimentoCaixa::efeitoNaGaveta).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Dinheiro somar(TipoMovimentoCaixa tipo) {
        return movimentos.stream()
                .filter(movimento -> movimento.tipo() == tipo)
                .map(MovimentoCaixa::valor)
                .reduce(Dinheiro.zero(), Dinheiro::somar);
    }

    private void garantirAberta() {
        if (status != StatusSessaoCaixa.ABERTA) {
            throw new DomainException("Este caixa já foi fechado");
        }
    }

    public UUID id() {
        return id;
    }

    public String operador() {
        return operador;
    }

    public Optional<UUID> pontoCaixaId() {
        return Optional.ofNullable(pontoCaixaId);
    }

    public List<ConferenciaForma> conferenciasForma() {
        return Collections.unmodifiableList(conferenciasForma);
    }

    public StatusSessaoCaixa status() {
        return status;
    }

    public Instant abertaEm() {
        return abertaEm;
    }

    public String abertaPor() {
        return abertaPor;
    }

    public Dinheiro fundoInicial() {
        return fundoInicial;
    }

    public ContagemCedulas cedulasAbertura() {
        return new ContagemCedulas(cedulasAbertura);
    }

    public List<MovimentoCaixa> movimentos() {
        return Collections.unmodifiableList(movimentos);
    }

    public Optional<Instant> fechadaEm() {
        return Optional.ofNullable(fechadaEm);
    }

    public Optional<String> fechadaPor() {
        return Optional.ofNullable(fechadaPor);
    }

    public ContagemCedulas cedulasFechamento() {
        return new ContagemCedulas(cedulasFechamento);
    }

    public Optional<Dinheiro> valorContado() {
        return Optional.ofNullable(valorContado);
    }

    public Optional<Dinheiro> vendasEmDinheiro() {
        return Optional.ofNullable(vendasEmDinheiro);
    }

    public Optional<BigDecimal> valorEsperado() {
        return Optional.ofNullable(valorEsperado);
    }

    public Optional<String> observacaoFechamento() {
        return Optional.ofNullable(observacaoFechamento);
    }
}

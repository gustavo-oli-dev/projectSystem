package com.empresax.sistema.pedido;

import com.empresax.sistema.common.domain.DomainException;
import com.empresax.sistema.documentofiscal.TipoDocumentoFiscal;
import com.empresax.sistema.shared.dinheiro.Dinheiro;
import com.empresax.sistema.shared.documento.Cpf;
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
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

/**
 * Agregado raiz do pedido de venda (RF05). Um pedido com item de produto e item de serviço gera
 * dois DocumentoFiscal independentes (NF-e e NFS-e) — ver docs/MODELO-DOMINIO.md.
 */
@Entity
@Table(name = "pedidos")
public class Pedido {

    private static final int CASAS_DO_REAL = 2;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Ausente só na venda de balcão (consumidor não identificado) — o banco garante com CHECK. */
    @Column
    private UUID clienteId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private CanalVenda canal;

    /** "CPF na nota" opcional do consumidor no balcão (vai para a NFC-e). */
    @Column(length = 11, updatable = false)
    private String cpfNaNota;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusPedido status;

    @ElementCollection
    @CollectionTable(name = "itens_pedido", joinColumns = @JoinColumn(name = "pedido_id"))
    private List<ItemPedido> itens = new ArrayList<>();

    @Column(nullable = false, updatable = false)
    private Instant criadoEm;

    /** Caixa aberto em que a venda de balcão foi feita (o fechamento soma as vendas por ele). */
    @Column(updatable = false)
    private UUID sessaoCaixaId;

    /** Momento da venda de fato (confirmação = baixa no estoque). É a data que os relatórios usam. */
    @Column
    private Instant confirmadoEm;

    /** Gerente que autorizou o desconto (vazio = venda sem desconto). */
    @Column
    private String descontoAutorizadoPor;

    protected Pedido() {
        // exigido pelo JPA
    }

    public Pedido(UUID clienteId, List<ItemPedido> itensIniciais) {
        this(validarCliente(clienteId), CanalVenda.PAINEL, null, itensIniciais);
    }

    private Pedido(UUID clienteId, CanalVenda canal, Cpf cpfNaNota, List<ItemPedido> itensIniciais) {
        this.clienteId = clienteId;
        this.canal = canal;
        this.cpfNaNota = cpfNaNota == null ? null : cpfNaNota.valor();
        this.itens = new ArrayList<>(validarItens(itensIniciais));
        this.status = StatusPedido.ABERTO;
        this.criadoEm = Instant.now();
    }

    /**
     * Venda presencial: o cliente cadastrado é opcional (clienteId pode ser nulo), assim como o CPF na
     * nota. O caixa aberto é obrigatório — não existe venda de balcão fora de um caixa.
     */
    public static Pedido noBalcao(List<ItemPedido> itens, Cpf cpfNaNota, UUID clienteId, UUID sessaoCaixaId) {
        if (sessaoCaixaId == null) {
            throw new DomainException("Abra o caixa antes de vender");
        }
        Pedido pedido = new Pedido(clienteId, CanalVenda.BALCAO, cpfNaNota, itens);
        pedido.sessaoCaixaId = sessaoCaixaId;
        return pedido;
    }

    private static UUID validarCliente(UUID clienteId) {
        if (clienteId == null) {
            throw new DomainException("Pedido precisa de um cliente");
        }
        return clienteId;
    }

    private static List<ItemPedido> validarItens(List<ItemPedido> itens) {
        if (itens == null || itens.isEmpty()) {
            throw new DomainException("Pedido precisa de ao menos um item");
        }
        return itens;
    }

    public void adicionarItem(ItemPedido item) {
        garantirAberto();
        if (item == null) {
            throw new DomainException("Item de pedido é obrigatório");
        }
        itens.add(item);
    }

    public void confirmar() {
        garantirAberto();
        if (itens.isEmpty()) {
            throw new DomainException("Pedido sem itens não pode ser confirmado");
        }
        this.status = StatusPedido.AGUARDANDO_EMISSAO;
        this.confirmadoEm = Instant.now();
    }

    public void concluir() {
        if (status != StatusPedido.AGUARDANDO_EMISSAO) {
            throw new DomainException("Pedido só é concluído depois de confirmado e com nota emitida");
        }
        this.status = StatusPedido.CONCLUIDO;
    }

    public void cancelar() {
        if (status == StatusPedido.CONCLUIDO) {
            throw new DomainException("Pedido concluído não pode ser cancelado");
        }
        this.status = StatusPedido.CANCELADO;
    }

    /** Confirmar é o que tira os produtos do estoque; antes disso nada saiu. */
    public boolean produtosSairamDoEstoque() {
        return status == StatusPedido.AGUARDANDO_EMISSAO || status == StatusPedido.CONCLUIDO;
    }

    /** Só produtos têm estoque — serviço não. */
    public List<ItemPedido> itensDeProduto() {
        return itens.stream().filter(item -> item.tipo() == TipoItem.PRODUTO).toList();
    }

    private void garantirAberto() {
        if (status != StatusPedido.ABERTO) {
            throw new DomainException("Pedido não está aberto para alteração");
        }
    }

    public Dinheiro valorTotal() {
        return itens.stream()
                .map(ItemPedido::subtotal)
                .reduce(Dinheiro.zero(), Dinheiro::somar);
    }

    /** Soma dos itens antes do desconto. */
    public Dinheiro valorBruto() {
        return itens.stream()
                .map(ItemPedido::valorBruto)
                .reduce(Dinheiro.zero(), Dinheiro::somar);
    }

    /** Desconto autorizado pelo gerente (D35). A promoção fica à parte, em descontoPromocao(). */
    public Dinheiro desconto() {
        return somar(ItemPedido::descontoDoGerente);
    }

    /** Quanto a venda ganhou nas promoções dos produtos (D38). */
    public Dinheiro descontoPromocao() {
        return somar(ItemPedido::descontoPromocao);
    }

    /** Soma dos itens com a promoção, antes do desconto do gerente. */
    public Dinheiro valorComPromocao() {
        return somar(ItemPedido::valorComPromocao);
    }

    private Dinheiro somar(Function<ItemPedido, Dinheiro> valorDoItem) {
        return itens.stream().map(valorDoItem).reduce(Dinheiro.zero(), Dinheiro::somar);
    }

    public Optional<String> descontoAutorizadoPor() {
        return Optional.ofNullable(descontoAutorizadoPor);
    }

    /**
     * Desconto na venda inteira, autorizado por um gerente (D35). É rateado pelos itens na proporção
     * do valor de cada um (a NFC-e exige o desconto por item); os centavos que sobram do
     * arredondamento vão para os itens que ainda comportam, para a soma bater exatamente.
     */
    public void aplicarDesconto(Dinheiro total, String autorizadoPor) {
        garantirAberto();
        if (autorizadoPor == null || autorizadoPor.isBlank()) {
            throw new DomainException("Desconto precisa da autorização de um gerente");
        }
        // Rateado sobre o valor já com promoção: o desconto do gerente vem depois dela.
        BigDecimal bruto = valorComPromocao().valor();
        if (total == null || total.valor().signum() == 0) {
            throw new DomainException("O desconto precisa ser maior que zero");
        }
        if (total.valor().compareTo(bruto) >= 0) {
            throw new DomainException("O desconto não pode ser igual ou maior que o total da venda");
        }
        BigDecimal[] partes = new BigDecimal[itens.size()];
        BigDecimal distribuido = BigDecimal.ZERO;
        for (int indice = 0; indice < itens.size(); indice++) {
            partes[indice] = total.valor().multiply(itens.get(indice).valorComPromocao().valor())
                    .divide(bruto, CASAS_DO_REAL, RoundingMode.DOWN);
            distribuido = distribuido.add(partes[indice]);
        }
        BigDecimal sobra = total.valor().subtract(distribuido);
        for (int indice = 0; indice < itens.size() && sobra.signum() > 0; indice++) {
            BigDecimal espaco = itens.get(indice).valorComPromocao().valor().subtract(partes[indice]);
            BigDecimal acrescimo = espaco.min(sobra);
            partes[indice] = partes[indice].add(acrescimo);
            sobra = sobra.subtract(acrescimo);
        }
        for (int indice = 0; indice < itens.size(); indice++) {
            itens.get(indice).receberDesconto(new Dinheiro(partes[indice]));
        }
        this.descontoAutorizadoPor = autorizadoPor;
    }

    /**
     * Um pedido com produto e serviço exige dois documentos independentes (NF-e e NFS-e); só com
     * produtos, apenas NF-e; só com serviços, apenas NFS-e.
     */
    public Set<TipoDocumentoFiscal> documentosFiscaisNecessarios() {
        Set<TipoDocumentoFiscal> tipos = EnumSet.noneOf(TipoDocumentoFiscal.class);
        for (ItemPedido item : itens) {
            tipos.add(canal.documentoPara(item.tipo()));
        }
        return Collections.unmodifiableSet(tipos);
    }

    public UUID id() {
        return id;
    }

    public Optional<UUID> clienteId() {
        return Optional.ofNullable(clienteId);
    }

    public Optional<UUID> sessaoCaixaId() {
        return Optional.ofNullable(sessaoCaixaId);
    }

    public CanalVenda canal() {
        return canal;
    }

    public boolean vendidoNoBalcao() {
        return canal == CanalVenda.BALCAO;
    }

    public Optional<String> cpfNaNota() {
        return Optional.ofNullable(cpfNaNota);
    }

    public StatusPedido status() {
        return status;
    }

    public List<ItemPedido> itens() {
        return Collections.unmodifiableList(itens);
    }

    public Instant criadoEm() {
        return criadoEm;
    }

    public Optional<Instant> confirmadoEm() {
        return Optional.ofNullable(confirmadoEm);
    }
}

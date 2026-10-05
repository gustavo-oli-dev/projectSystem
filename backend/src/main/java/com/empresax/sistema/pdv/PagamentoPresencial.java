package com.empresax.sistema.pdv;

import com.empresax.sistema.common.domain.DomainException;
import com.empresax.sistema.shared.dinheiro.Dinheiro;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Pagamento de uma venda de balcão. Guarda o que a NFC-e exige no grupo de pagamento: forma,
 * bandeira, código de autorização e se a maquininha estava integrada ao sistema (D19).
 *
 * Maquininha integrada (padrão, D20): o valor vai para a maquininha e o resultado volta sozinho.
 * Contingência (maquininha sem conexão): o operador digita a autorização — fica integrado = false.
 */
@Entity
@Table(name = "pagamentos_presenciais")
public class PagamentoPresencial {

    private static final Pattern CODIGO_AUTORIZACAO_VALIDO = Pattern.compile("[A-Za-z0-9]{4,20}");

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, updatable = false)
    private UUID pedidoId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private FormaPagamentoPresencial forma;

    @Column(nullable = false, updatable = false)
    private Dinheiro valor;

    /** Só em dinheiro: quanto o consumidor entregou (o troco sai daqui). */
    @Column(updatable = false)
    private Dinheiro valorRecebido;

    /** Preenchidos pelo operador (contingência) ou pela maquininha integrada ao aprovar. */
    @Enumerated(EnumType.STRING)
    @Column
    private BandeiraCartao bandeira;

    @Column(length = 40)
    private String codigoAutorizacao;

    /** Cobrança em andamento na maquininha integrada (muda a cada nova tentativa). */
    @Column
    private String idTransacaoMaquininha;

    /** Pagamento aprovado no fornecedor — usado para estornar automaticamente. */
    @Column
    private String idPagamentoProvedor;

    @Column(nullable = false, updatable = false)
    private boolean maquininhaIntegrada;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusPagamentoPresencial status;

    @Column(nullable = false, updatable = false)
    private String operador;

    @Column(nullable = false, updatable = false)
    private Instant criadoEm;

    @Column
    private Instant estornadoEm;

    protected PagamentoPresencial() {
        // exigido pelo JPA
    }

    private PagamentoPresencial(UUID pedidoId, FormaPagamentoPresencial forma, Dinheiro valor, String operador) {
        if (pedidoId == null || forma == null || valor == null) {
            throw new DomainException("Pagamento precisa de venda, forma e valor");
        }
        if (operador == null || operador.isBlank()) {
            throw new DomainException("Pagamento precisa do operador do caixa");
        }
        this.pedidoId = pedidoId;
        this.forma = forma;
        this.valor = valor;
        this.operador = operador;
        this.status = StatusPagamentoPresencial.APROVADO;
        this.criadoEm = Instant.now();
    }

    public static PagamentoPresencial emDinheiro(UUID pedidoId, Dinheiro valor, Dinheiro valorRecebido, String operador) {
        if (valorRecebido == null || valorRecebido.menorQue(valor)) {
            throw new DomainException("Valor recebido em dinheiro é menor que o total da venda");
        }
        PagamentoPresencial pagamento = new PagamentoPresencial(pedidoId, FormaPagamentoPresencial.DINHEIRO, valor, operador);
        pagamento.valorRecebido = valorRecebido;
        return pagamento;
    }

    /** Cartão ou Pix na maquininha avulsa: operador confirma na maquininha e digita a autorização. */
    public static PagamentoPresencial naMaquininhaAvulsa(
            UUID pedidoId, FormaPagamentoPresencial forma, Dinheiro valor, BandeiraCartao bandeira,
            String codigoAutorizacao, String operador
    ) {
        if (forma == null || !forma.passaPelaMaquininha()) {
            throw new DomainException("Forma de pagamento não passa pela maquininha");
        }
        if (forma.exigeBandeira() && bandeira == null) {
            throw new DomainException("Informe a bandeira do cartão");
        }
        if (codigoAutorizacao == null || !CODIGO_AUTORIZACAO_VALIDO.matcher(codigoAutorizacao.trim()).matches()) {
            throw new DomainException("Digite o código de autorização impresso no comprovante da maquininha");
        }
        PagamentoPresencial pagamento = new PagamentoPresencial(pedidoId, forma, valor, operador);
        pagamento.bandeira = forma.exigeBandeira() ? bandeira : null;
        pagamento.codigoAutorizacao = codigoAutorizacao.trim().toUpperCase();
        pagamento.maquininhaIntegrada = false;
        return pagamento;
    }

    /**
     * Maquininha integrada: o valor já está na tela da maquininha, esperando o cartão. Bandeira e
     * autorização chegam da própria maquininha ao aprovar — ninguém digita.
     */
    public static PagamentoPresencial aguardandoMaquininha(
            UUID pedidoId, FormaPagamentoPresencial forma, Dinheiro valor, String idTransacao, String operador
    ) {
        if (forma == null || !forma.exigeBandeira()) {
            throw new DomainException("A maquininha integrada cobra cartão de crédito ou débito");
        }
        PagamentoPresencial pagamento = new PagamentoPresencial(pedidoId, forma, valor, operador);
        pagamento.status = StatusPagamentoPresencial.AGUARDANDO_MAQUININHA;
        pagamento.maquininhaIntegrada = true;
        pagamento.idTransacaoMaquininha = exigirIdTransacao(idTransacao);
        return pagamento;
    }

    public void confirmarPelaMaquininha(BandeiraCartao bandeiraLida, String autorizacao, String idPagamento) {
        garantirStatus(StatusPagamentoPresencial.AGUARDANDO_MAQUININHA, "Este pagamento não está aguardando a maquininha");
        this.bandeira = bandeiraLida;
        this.codigoAutorizacao = autorizacao;
        this.idPagamentoProvedor = idPagamento;
        this.status = StatusPagamentoPresencial.APROVADO;
    }

    public void recusarPelaMaquininha() {
        garantirStatus(StatusPagamentoPresencial.AGUARDANDO_MAQUININHA, "Este pagamento não está aguardando a maquininha");
        this.status = StatusPagamentoPresencial.RECUSADO;
    }

    /** Cartão recusado: manda o valor de novo para a maquininha (outro cartão, por exemplo). */
    public void novaTentativaNaMaquininha(String novoIdTransacao) {
        garantirStatus(StatusPagamentoPresencial.RECUSADO, "Só dá para tentar de novo depois de uma recusa");
        this.idTransacaoMaquininha = exigirIdTransacao(novoIdTransacao);
        this.status = StatusPagamentoPresencial.AGUARDANDO_MAQUININHA;
    }

    /** Cliente desistiu antes de pagar: nada foi cobrado, nada a estornar. */
    public void cancelarAntesDoPagamento() {
        if (status != StatusPagamentoPresencial.AGUARDANDO_MAQUININHA && status != StatusPagamentoPresencial.RECUSADO) {
            throw new DomainException("Este pagamento já foi concluído — use o estorno");
        }
        this.status = StatusPagamentoPresencial.CANCELADO;
    }

    public void estornar() {
        garantirStatus(StatusPagamentoPresencial.APROVADO, "Só um pagamento aprovado pode ser estornado");
        this.status = StatusPagamentoPresencial.ESTORNADO;
        this.estornadoEm = Instant.now();
    }

    public boolean aguardandoMaquininha() {
        return status == StatusPagamentoPresencial.AGUARDANDO_MAQUININHA;
    }

    public boolean aprovado() {
        return status == StatusPagamentoPresencial.APROVADO;
    }

    public boolean recusado() {
        return status == StatusPagamentoPresencial.RECUSADO;
    }

    private void garantirStatus(StatusPagamentoPresencial esperado, String mensagem) {
        if (status != esperado) {
            throw new DomainException(mensagem);
        }
    }

    private static String exigirIdTransacao(String idTransacao) {
        if (idTransacao == null || idTransacao.isBlank()) {
            throw new DomainException("A maquininha não devolveu o identificador da cobrança");
        }
        return idTransacao;
    }

    public Optional<Dinheiro> troco() {
        return Optional.ofNullable(valorRecebido).map(recebido -> recebido.subtrair(valor));
    }

    public UUID id() {
        return id;
    }

    public UUID pedidoId() {
        return pedidoId;
    }

    public FormaPagamentoPresencial forma() {
        return forma;
    }

    public Dinheiro valor() {
        return valor;
    }

    public Optional<Dinheiro> valorRecebido() {
        return Optional.ofNullable(valorRecebido);
    }

    public Optional<BandeiraCartao> bandeira() {
        return Optional.ofNullable(bandeira);
    }

    public Optional<String> codigoAutorizacao() {
        return Optional.ofNullable(codigoAutorizacao);
    }

    public Optional<String> idTransacaoMaquininha() {
        return Optional.ofNullable(idTransacaoMaquininha);
    }

    public Optional<String> idPagamentoProvedor() {
        return Optional.ofNullable(idPagamentoProvedor);
    }

    public boolean maquininhaIntegrada() {
        return maquininhaIntegrada;
    }

    public StatusPagamentoPresencial status() {
        return status;
    }

    public String operador() {
        return operador;
    }

    public Instant criadoEm() {
        return criadoEm;
    }
}

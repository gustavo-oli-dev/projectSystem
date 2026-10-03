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
 * Por enquanto o código de autorização é digitado pelo operador (maquininha avulsa, integrado =
 * false); quando o fornecedor for escolhido, a maquininha integrada preencherá esses dados.
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

    @Enumerated(EnumType.STRING)
    @Column(updatable = false)
    private BandeiraCartao bandeira;

    @Column(updatable = false, length = 20)
    private String codigoAutorizacao;

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

    public void estornar() {
        if (status == StatusPagamentoPresencial.ESTORNADO) {
            throw new DomainException("Este pagamento já foi estornado");
        }
        this.status = StatusPagamentoPresencial.ESTORNADO;
        this.estornadoEm = Instant.now();
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

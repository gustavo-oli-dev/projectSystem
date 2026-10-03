package com.empresax.sistema.cobranca;

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
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.UUID;

/**
 * Cobrança de um Pedido confirmado (RF06). A baixa (marcarComoPaga) acontece pelo
 * WebhookPagamentoProcessor, nunca direto pelo payload do webhook — sempre após confirmar o
 * status com uma consulta à API do provedor (ver DECISOES.md).
 */
@Entity
@Table(name = "cobrancas", uniqueConstraints = @UniqueConstraint(columnNames = "referencia_externa"))
public class Cobranca {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID pedidoId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MeioCobranca meio;

    @Column(nullable = false)
    private Dinheiro valor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusCobranca status;

    @Column(nullable = false, unique = true)
    private String referenciaExterna;

    @Column(nullable = false, updatable = false)
    private Instant criadoEm;

    @Column(nullable = false)
    private Instant atualizadoEm;

    protected Cobranca() {
        // exigido pelo JPA
    }

    public Cobranca(UUID pedidoId, MeioCobranca meio, Dinheiro valor, String referenciaExterna) {
        this.pedidoId = validarPedido(pedidoId);
        this.meio = validarMeio(meio);
        this.valor = validarValor(valor);
        this.referenciaExterna = validarReferencia(referenciaExterna);
        this.status = StatusCobranca.PENDENTE;
        this.criadoEm = Instant.now();
        this.atualizadoEm = this.criadoEm;
    }

    private static UUID validarPedido(UUID pedidoId) {
        if (pedidoId == null) {
            throw new DomainException("Cobrança precisa estar vinculada a um pedido");
        }
        return pedidoId;
    }

    private static MeioCobranca validarMeio(MeioCobranca meio) {
        if (meio == null) {
            throw new DomainException("Meio de cobrança (Pix ou boleto) é obrigatório");
        }
        return meio;
    }

    private static Dinheiro validarValor(Dinheiro valor) {
        if (valor == null) {
            throw new DomainException("Valor da cobrança é obrigatório");
        }
        return valor;
    }

    private static String validarReferencia(String referenciaExterna) {
        if (referenciaExterna == null || referenciaExterna.isBlank()) {
            throw new DomainException("Referência externa (provedor de pagamento) é obrigatória");
        }
        return referenciaExterna;
    }

    public void marcarComoPaga() {
        garantirPendente();
        this.status = StatusCobranca.PAGA;
        this.atualizadoEm = Instant.now();
    }

    public void marcarComoVencida() {
        garantirPendente();
        this.status = StatusCobranca.VENCIDA;
        this.atualizadoEm = Instant.now();
    }

    public void cancelar() {
        if (status == StatusCobranca.PAGA || status == StatusCobranca.REEMBOLSADA) {
            throw new DomainException("Cobrança já paga não pode ser cancelada — use o reembolso");
        }
        this.status = StatusCobranca.CANCELADA;
        this.atualizadoEm = Instant.now();
    }

    /** Só o que foi pago pode ser reembolsado, e uma vez só. */
    public void marcarComoReembolsada() {
        if (status != StatusCobranca.PAGA) {
            throw new DomainException("Só uma cobrança paga pode ser reembolsada");
        }
        this.status = StatusCobranca.REEMBOLSADA;
        this.atualizadoEm = Instant.now();
    }

    public boolean foiPaga() {
        return status == StatusCobranca.PAGA;
    }

    public boolean aguardandoPagamento() {
        return status == StatusCobranca.PENDENTE;
    }

    private void garantirPendente() {
        if (status != StatusCobranca.PENDENTE) {
            throw new DomainException("Cobrança já foi processada e não pode mudar de estado");
        }
    }

    public UUID id() {
        return id;
    }

    public UUID pedidoId() {
        return pedidoId;
    }

    public MeioCobranca meio() {
        return meio;
    }

    public Dinheiro valor() {
        return valor;
    }

    public StatusCobranca status() {
        return status;
    }

    public String referenciaExterna() {
        return referenciaExterna;
    }

    public Instant criadoEm() {
        return criadoEm;
    }

    public Instant atualizadoEm() {
        return atualizadoEm;
    }
}

package com.empresax.sistema.cobranca.webhook;

import com.empresax.sistema.cobranca.Cobranca;
import com.empresax.sistema.cobranca.CobrancaRepository;
import com.empresax.sistema.cobranca.pagamento.ProvedorPagamento;
import com.empresax.sistema.cobranca.pagamento.StatusPagamentoExterno;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Processa o outbox de eventos de webhook de pagamento (decisão D4 em DECISOES.md): nunca confia
 * no payload recebido, sempre confirma o status consultando a API do provedor antes de dar baixa.
 */
@Component
public class WebhookPagamentoProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(WebhookPagamentoProcessor.class);
    private static final long INTERVALO_ENTRE_EXECUCOES_MS = 10_000;

    private final EventoWebhookPagamentoRepository eventoRepository;
    private final CobrancaRepository cobrancaRepository;
    private final ProvedorPagamento provedorPagamento;

    public WebhookPagamentoProcessor(
            EventoWebhookPagamentoRepository eventoRepository,
            CobrancaRepository cobrancaRepository,
            ProvedorPagamento provedorPagamento
    ) {
        this.eventoRepository = eventoRepository;
        this.cobrancaRepository = cobrancaRepository;
        this.provedorPagamento = provedorPagamento;
    }

    @Scheduled(fixedDelay = INTERVALO_ENTRE_EXECUCOES_MS)
    @Transactional
    public void processarPendentes() {
        List<EventoWebhookPagamento> pendentes =
                eventoRepository.findByStatusOrderByRecebidoEmAsc(StatusEventoWebhook.PENDENTE);
        pendentes.forEach(this::processar);
    }

    private void processar(EventoWebhookPagamento evento) {
        try {
            StatusPagamentoExterno status = provedorPagamento.consultarStatus(evento.referenciaExterna());
            atualizarCobranca(evento.referenciaExterna(), status);
            evento.marcarComoProcessado();
        } catch (RuntimeException falhaDeComunicacao) {
            evento.marcarComoFalhou();
        }
    }

    private void atualizarCobranca(String referenciaExterna, StatusPagamentoExterno status) {
        cobrancaRepository.findByReferenciaExterna(referenciaExterna)
                .ifPresent(cobranca -> aplicarStatus(cobranca, status));
    }

    /**
     * Só cobrança ainda pendente muda por aviso do provedor. O Mercado Pago avisa de novo em várias
     * situações (inclusive depois de um reembolso); sem essa guarda, um aviso atrasado poderia
     * transformar uma cobrança reembolsada em "cancelada".
     */
    private void aplicarStatus(Cobranca cobranca, StatusPagamentoExterno status) {
        if (!cobranca.aguardandoPagamento()) {
            if (status == StatusPagamentoExterno.APROVADO && !cobranca.foiPaga()) {
                LOG.warn("Pagamento aprovado para a cobrança {} que não estava pendente ({}). Verificar reembolso.",
                        cobranca.id(), cobranca.status());
            }
            return;
        }
        cobranca.aplicarStatusDoProvedor(status);
    }
}

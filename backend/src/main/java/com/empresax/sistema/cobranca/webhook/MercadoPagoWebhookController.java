package com.empresax.sistema.cobranca.webhook;

import com.empresax.sistema.cobranca.pagamento.AssinaturaMercadoPagoValidador;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint público (justificado): o Mercado Pago não carrega nosso JWT, então a autenticidade da
 * chamada é garantida pela assinatura HMAC, não por Spring Security (ver SecurityConfig).
 */
@RestController
@RequestMapping("/api/webhooks/mercadopago")
public class MercadoPagoWebhookController {

    private final EventoWebhookPagamentoRepository eventoRepository;
    private final AssinaturaMercadoPagoValidador validadorAssinatura;

    public MercadoPagoWebhookController(
            EventoWebhookPagamentoRepository eventoRepository,
            AssinaturaMercadoPagoValidador validadorAssinatura
    ) {
        this.eventoRepository = eventoRepository;
        this.validadorAssinatura = validadorAssinatura;
    }

    @PostMapping
    public ResponseEntity<Void> receber(
            @RequestHeader(value = "x-signature", required = false) String assinatura,
            @RequestHeader(value = "x-request-id", required = false) String idRequisicao,
            @RequestParam(value = "data.id", required = false) String dataId
    ) {
        if (!validadorAssinatura.valida(assinatura, idRequisicao, dataId)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        registrarEvento(dataId);
        return ResponseEntity.ok().build();
    }

    private void registrarEvento(String referenciaExterna) {
        try {
            eventoRepository.save(new EventoWebhookPagamento(referenciaExterna));
        } catch (DataIntegrityViolationException notificacaoDuplicada) {
            // Idempotência: o Mercado Pago reenvia a notificação em caso de timeout — ignorar.
        }
    }
}

package com.empresax.sistema.atendimento.webhook;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Endpoints públicos (justificado): a W-API não carrega nosso JWT nem permite cabeçalho customizado —
 * só configura uma URL. A autenticidade vem de um token secreto na própria URL cadastrada no painel
 * da W-API (?token=...), comparado em tempo constante. Só grava o payload bruto (outbox).
 *
 * Um endpoint e um token por número (D17): o token do canal de clientes não abre o canal interno.
 */
@RestController
public class WApiWebhookController {

    private final EventoWebhookWhatsAppRepository eventoRepository;
    private final byte[] tokenAtendimento;
    private final byte[] tokenAssistente;

    public WApiWebhookController(
            EventoWebhookWhatsAppRepository eventoRepository,
            @Value("${whatsapp.wapi.webhook-token}") String tokenAtendimento,
            @Value("${assistente.whatsapp.webhook-token}") String tokenAssistente
    ) {
        this.eventoRepository = eventoRepository;
        this.tokenAtendimento = tokenAtendimento.getBytes(StandardCharsets.UTF_8);
        this.tokenAssistente = tokenAssistente.getBytes(StandardCharsets.UTF_8);
    }

    @PostMapping("/api/webhooks/wapi")
    public ResponseEntity<Void> receberAtendimento(
            @RequestParam(value = "token", required = false) String tokenRecebido,
            @RequestBody String payloadBruto
    ) {
        return registrar(CanalWhatsApp.ATENDIMENTO, tokenAtendimento, tokenRecebido, payloadBruto);
    }

    @PostMapping("/api/webhooks/wapi-assistente")
    public ResponseEntity<Void> receberAssistente(
            @RequestParam(value = "token", required = false) String tokenRecebido,
            @RequestBody String payloadBruto
    ) {
        return registrar(CanalWhatsApp.ASSISTENTE, tokenAssistente, tokenRecebido, payloadBruto);
    }

    private ResponseEntity<Void> registrar(
            CanalWhatsApp canal, byte[] tokenEsperado, String tokenRecebido, String payloadBruto
    ) {
        if (!tokenValido(tokenEsperado, tokenRecebido)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        eventoRepository.save(new EventoWebhookWhatsApp(canal, payloadBruto));
        return ResponseEntity.ok().build();
    }

    private static boolean tokenValido(byte[] tokenEsperado, String tokenRecebido) {
        if (tokenRecebido == null || tokenEsperado.length == 0) {
            return false;
        }
        return MessageDigest.isEqual(tokenEsperado, tokenRecebido.getBytes(StandardCharsets.UTF_8));
    }
}

package com.empresax.sistema.atendimento.bot;

import com.empresax.sistema.atendimento.conversa.ConversaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Orquestra a resposta automática. Se o bot falhar (IA ou WhatsApp fora, chave não configurada),
 * a conversa vai para a fila humana — o cliente nunca fica sem resposta em silêncio.
 */
@Service
public class AtendimentoBotService {

    private static final Logger LOG = LoggerFactory.getLogger(AtendimentoBotService.class);
    static final String MOTIVO_BOT_INDISPONIVEL = "Bot indisponível — precisa de atendimento humano";

    private final RespostaBotService respostaBotService;
    private final ConversaService conversaService;

    public AtendimentoBotService(RespostaBotService respostaBotService, ConversaService conversaService) {
        this.respostaBotService = respostaBotService;
        this.conversaService = conversaService;
    }

    public void responderSeAtivo(UUID conversaId) {
        try {
            respostaBotService.gerarEEnviar(conversaId);
        } catch (RuntimeException falha) {
            LOG.warn("Bot não conseguiu responder a conversa {}; transferindo para atendente", conversaId, falha);
            conversaService.transferirParaAtendente(conversaId, MOTIVO_BOT_INDISPONIVEL);
        }
    }
}

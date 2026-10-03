package com.empresax.sistema.atendimento.webhook;

import com.empresax.sistema.acesso.Permissao;
import com.empresax.sistema.assistentegestor.AssistenteGestorService;
import com.empresax.sistema.atendimento.bot.AtendimentoBotService;
import com.empresax.sistema.atendimento.mensagem.Mensagem;
import com.empresax.sistema.atendimento.mensagem.MensagemService;
import com.empresax.sistema.atendimento.whatsapp.ConfiguracaoWhatsApp;
import com.empresax.sistema.atendimento.whatsapp.WhatsAppGateway;
import org.springframework.beans.factory.annotation.Qualifier;
import com.empresax.sistema.usuario.Usuario;
import com.empresax.sistema.usuario.whatsapp.VinculoWhatsAppService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

/**
 * Processa o outbox de eventos do WhatsApp.
 *
 * Sem @Transactional no método agendado de propósito: cada etapa tem a própria transação. A mensagem
 * do cliente é salva e confirmada primeiro; só depois o bot roda (em transação separada) — senão o
 * bot não enxergaria a mensagem ainda não confirmada, e uma falha do bot desfaria a mensagem recebida.
 *
 * Dois números (D17): mensagem no número da empresa vira conversa de atendimento; mensagem no número
 * interno vai ao assistente do gestor, só para funcionários com WhatsApp verificado (ou o número do
 * dono configurado em ambiente), com as permissões do cargo de quem escreveu.
 */
@Component
public class WebhookWhatsAppProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(WebhookWhatsAppProcessor.class);
    private static final long INTERVALO_ENTRE_EXECUCOES_MS = 5_000;
    /** O WhatsApp do dono (configurado em ambiente) tem acesso irrestrito ao assistente. */
    private static final String SOLICITANTE_WHATSAPP_DONO = "WhatsApp do dono";
    private static final String SUFIXO_SOLICITANTE_WHATSAPP = " (WhatsApp)";
    private static final String MENSAGEM_SEM_ACESSO =
            "Seu acesso ao assistente da Empresa X não está mais ativo. Fale com o responsável.";

    private final EventoWebhookWhatsAppRepository eventoRepository;
    private final InterpretadorWebhookWApi interpretador;
    private final MensagemService mensagemService;
    private final AtendimentoBotService atendimentoBotService;
    private final AssistenteGestorService assistenteGestorService;
    private final WhatsAppGateway whatsAppAssistente;
    private final VinculoWhatsAppService vinculoWhatsAppService;
    private final ObjectMapper objectMapper;
    private final String telefoneDono;

    public WebhookWhatsAppProcessor(
            EventoWebhookWhatsAppRepository eventoRepository,
            InterpretadorWebhookWApi interpretador,
            MensagemService mensagemService,
            AtendimentoBotService atendimentoBotService,
            AssistenteGestorService assistenteGestorService,
            @Qualifier(ConfiguracaoWhatsApp.ASSISTENTE) WhatsAppGateway whatsAppAssistente,
            VinculoWhatsAppService vinculoWhatsAppService,
            @Value("${assistente.telefone-dono}") String telefoneDono
    ) {
        this.eventoRepository = eventoRepository;
        this.interpretador = interpretador;
        this.mensagemService = mensagemService;
        this.atendimentoBotService = atendimentoBotService;
        this.assistenteGestorService = assistenteGestorService;
        this.whatsAppAssistente = whatsAppAssistente;
        this.vinculoWhatsAppService = vinculoWhatsAppService;
        this.objectMapper = new ObjectMapper();
        this.telefoneDono = telefoneDono;
    }

    @Scheduled(fixedDelay = INTERVALO_ENTRE_EXECUCOES_MS)
    public void processarPendentes() {
        eventoRepository.findByStatusOrderByRecebidoEmAsc(StatusEventoWebhookWhatsApp.PENDENTE)
                .forEach(this::processar);
    }

    private void processar(EventoWebhookWhatsApp evento) {
        try {
            JsonNode payload = objectMapper.readTree(evento.payloadBruto());
            interpretador.interpretar(payload).ifPresent(recebida -> tratar(evento.canal(), recebida));
            evento.marcarComoProcessado();
        } catch (IOException | RuntimeException falha) {
            LOG.warn("Falha ao processar evento de webhook do WhatsApp {}", evento.id(), falha);
            evento.marcarComoFalhou();
        }
        eventoRepository.save(evento);
    }

    /**
     * O canal decide, nunca o remetente: no número da empresa todo mundo é cliente (até funcionário),
     * então dado gerencial nunca sai por ele.
     */
    private void tratar(CanalWhatsApp canal, MensagemRecebida recebida) {
        if (canal == CanalWhatsApp.ASSISTENTE) {
            tratarNoNumeroInterno(recebida);
            return;
        }
        Mensagem mensagem = mensagemService.registrarRecebida(
                recebida.telefoneRemetente(), recebida.texto(), recebida.idExterno());
        atendimentoBotService.responderSeAtivo(mensagem.conversaId());
    }

    /** Número desconhecido no canal interno é ignorado sem resposta — nem confirma para que serve o número. */
    private void tratarNoNumeroInterno(MensagemRecebida recebida) {
        Optional<Usuario> funcionario = vinculoWhatsAppService.buscarVinculado(recebida.telefoneRemetente());
        if (funcionario.isPresent()) {
            responderFuncionario(funcionario.get(), recebida);
            return;
        }
        if (recebida.telefoneRemetente().equals(telefoneDono)) {
            responderAssistente(recebida, EnumSet.allOf(Permissao.class), SOLICITANTE_WHATSAPP_DONO);
            return;
        }
        LOG.info("Mensagem de número não vinculado ignorada no WhatsApp interno");
    }

    /** Permissões relidas a cada mensagem: quem foi desativado ou trocou de cargo perde o acesso na hora. */
    private void responderFuncionario(Usuario funcionario, MensagemRecebida recebida) {
        if (!funcionario.podeUsarAssistentePeloWhatsApp()) {
            whatsAppAssistente.enviarTexto(recebida.telefoneRemetente(), MENSAGEM_SEM_ACESSO);
            return;
        }
        responderAssistente(recebida, funcionario.permissoes(), funcionario.email() + SUFIXO_SOLICITANTE_WHATSAPP);
    }

    private void responderAssistente(MensagemRecebida recebida, Set<Permissao> permissoes, String solicitante) {
        String resposta = assistenteGestorService.responder(recebida.texto(), permissoes, solicitante);
        whatsAppAssistente.enviarTexto(recebida.telefoneRemetente(), resposta);
    }
}

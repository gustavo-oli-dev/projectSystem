package com.empresax.sistema.atendimento.webhook;

import com.empresax.sistema.acesso.Cargo;
import com.empresax.sistema.acesso.Permissao;
import com.empresax.sistema.assistentegestor.AssistenteGestorService;
import com.empresax.sistema.atendimento.bot.AtendimentoBotService;
import com.empresax.sistema.atendimento.mensagem.Mensagem;
import com.empresax.sistema.atendimento.mensagem.MensagemService;
import com.empresax.sistema.atendimento.whatsapp.WhatsAppGateway;
import com.empresax.sistema.usuario.Usuario;
import com.empresax.sistema.usuario.whatsapp.TelefoneWhatsApp;
import com.empresax.sistema.usuario.whatsapp.VinculoWhatsAppService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** Regra anti-vazamento (D17): é o número em que a mensagem chegou que decide o destino, nunca o remetente. */
class WebhookWhatsAppProcessorTest {

    private static final String TELEFONE_FUNCIONARIO = "5585988887777";
    private static final String TELEFONE_DONO_CONFIGURADO = "5585900000000";
    private static final String PAYLOAD = """
            {"sender":{"id":"5585988887777@s.whatsapp.net"},
             "msgContent":{"conversation":"Como está o faturamento?"},
             "messageId":"msg-1"}
            """;

    private final EventoWebhookWhatsAppRepository eventoRepository = mock(EventoWebhookWhatsAppRepository.class);
    private final MensagemService mensagemService = mock(MensagemService.class);
    private final AtendimentoBotService atendimentoBotService = mock(AtendimentoBotService.class);
    private final AssistenteGestorService assistenteGestorService = mock(AssistenteGestorService.class);
    private final WhatsAppGateway whatsAppInterno = mock(WhatsAppGateway.class);
    private final VinculoWhatsAppService vinculoWhatsAppService = mock(VinculoWhatsAppService.class);

    private WebhookWhatsAppProcessor processor;

    @BeforeEach
    void preparar() {
        processor = new WebhookWhatsAppProcessor(
                eventoRepository, new InterpretadorWebhookWApi(), mensagemService, atendimentoBotService,
                assistenteGestorService, whatsAppInterno, vinculoWhatsAppService, TELEFONE_DONO_CONFIGURADO);
    }

    @Test
    void funcionarioVinculadoQueEscreveNoNumeroDaEmpresaEhTratadoComoCliente() {
        funcionarioVinculado(Set.of(Permissao.ASSISTENTE_GESTOR_USAR, Permissao.FATURAMENTO_VER));
        Mensagem mensagem = mock(Mensagem.class);
        when(mensagem.conversaId()).thenReturn(UUID.randomUUID());
        when(mensagemService.registrarRecebida(eq(TELEFONE_FUNCIONARIO), anyString(), any())).thenReturn(mensagem);

        processarEventoNoCanal(CanalWhatsApp.ATENDIMENTO);

        verify(mensagemService).registrarRecebida(eq(TELEFONE_FUNCIONARIO), anyString(), any());
        verifyNoInteractions(assistenteGestorService, whatsAppInterno);
    }

    @Test
    void numeroNaoVinculadoNoNumeroInternoEhIgnoradoSemResposta() {
        when(vinculoWhatsAppService.buscarVinculado(TELEFONE_FUNCIONARIO)).thenReturn(Optional.empty());

        processarEventoNoCanal(CanalWhatsApp.ASSISTENTE);

        verifyNoInteractions(assistenteGestorService, whatsAppInterno, mensagemService);
    }

    @Test
    void funcionarioVinculadoNoNumeroInternoRecebeRespostaComAsPermissoesDoCargo() {
        Set<Permissao> permissoesDoCargo = Set.of(Permissao.ASSISTENTE_GESTOR_USAR, Permissao.PEDIDOS_VER);
        funcionarioVinculado(permissoesDoCargo);
        when(assistenteGestorService.responder(anyString(), any(), anyString())).thenReturn("12 pedidos em aberto");

        processarEventoNoCanal(CanalWhatsApp.ASSISTENTE);

        verify(assistenteGestorService).responder(anyString(), eq(permissoesDoCargo), anyString());
        verify(whatsAppInterno).enviarTexto(TELEFONE_FUNCIONARIO, "12 pedidos em aberto");
        verify(mensagemService, never()).registrarRecebida(anyString(), anyString(), any());
    }

    @Test
    void funcionarioDesativadoNoNumeroInternoNaoChegaAoAssistente() {
        funcionarioVinculado(Set.of(Permissao.ASSISTENTE_GESTOR_USAR, Permissao.FATURAMENTO_VER))
                .desativar();

        processarEventoNoCanal(CanalWhatsApp.ASSISTENTE);

        verifyNoInteractions(assistenteGestorService);
        verify(whatsAppInterno).enviarTexto(eq(TELEFONE_FUNCIONARIO), anyString());
    }

    private Usuario funcionarioVinculado(Set<Permissao> permissoes) {
        Usuario funcionario = Usuario.comCargo(
                "Gerente", "gerente@empresax.com", "hash", new Cargo("Gerente", null, permissoes));
        funcionario.vincularWhatsApp(TelefoneWhatsApp.de(TELEFONE_FUNCIONARIO));
        when(vinculoWhatsAppService.buscarVinculado(TELEFONE_FUNCIONARIO)).thenReturn(Optional.of(funcionario));
        return funcionario;
    }

    private void processarEventoNoCanal(CanalWhatsApp canal) {
        when(eventoRepository.findByStatusOrderByRecebidoEmAsc(StatusEventoWebhookWhatsApp.PENDENTE))
                .thenReturn(List.of(new EventoWebhookWhatsApp(canal, PAYLOAD)));
        processor.processarPendentes();
    }
}

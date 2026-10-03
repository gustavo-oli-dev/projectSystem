package com.empresax.sistema.atendimento.mensagem;

import com.empresax.sistema.atendimento.conversa.Conversa;
import com.empresax.sistema.atendimento.conversa.ConversaService;
import com.empresax.sistema.atendimento.whatsapp.ConfiguracaoWhatsApp;
import com.empresax.sistema.atendimento.whatsapp.WhatsAppGateway;
import org.springframework.beans.factory.annotation.Qualifier;
import com.empresax.sistema.common.domain.DomainException;
import com.empresax.sistema.shared.armazenamento.ArmazenamentoObjetos;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
public class MensagemService {

    private final MensagemRepository mensagemRepository;
    private final AnexoRepository anexoRepository;
    private final ConversaService conversaService;
    private final WhatsAppGateway whatsAppGateway;
    private final ArmazenamentoObjetos armazenamentoObjetos;

    public MensagemService(
            MensagemRepository mensagemRepository,
            AnexoRepository anexoRepository,
            ConversaService conversaService,
            @Qualifier(ConfiguracaoWhatsApp.ATENDIMENTO) WhatsAppGateway whatsAppGateway,
            ArmazenamentoObjetos armazenamentoObjetos
    ) {
        this.mensagemRepository = mensagemRepository;
        this.anexoRepository = anexoRepository;
        this.conversaService = conversaService;
        this.whatsAppGateway = whatsAppGateway;
        this.armazenamentoObjetos = armazenamentoObjetos;
    }

    /** Atendente responde: responder já significa assumir a conversa (o bot para). */
    @Transactional
    public Mensagem enviarPorAtendente(UUID conversaId, String conteudo, String emailAtendente) {
        Conversa conversa = conversaService.assumir(conversaId, emailAtendente);
        String idExterno = whatsAppGateway.enviarTexto(conversa.telefoneWhatsapp(), conteudo);
        return mensagemRepository.save(new Mensagem(conversaId, OrigemMensagem.ATENDENTE, conteudo, idExterno));
    }

    @Transactional
    public Mensagem registrarRespostaDoBot(UUID conversaId, String conteudo, String idExterno) {
        return mensagemRepository.save(new Mensagem(conversaId, OrigemMensagem.BOT, conteudo, idExterno));
    }

    @Transactional
    public Mensagem registrarRecebida(String telefoneRemetente, String conteudo, String idExternoWhatsapp) {
        Conversa conversa = conversaService.obterOuCriarAberta(telefoneRemetente);
        Mensagem mensagem = new Mensagem(conversa.id(), OrigemMensagem.CLIENTE, conteudo, idExternoWhatsapp);
        return mensagemRepository.save(mensagem);
    }

    @Transactional
    public Anexo registrarAnexo(UUID mensagemId, byte[] conteudo, String mimeType, String nomeArquivoOriginal) {
        String chaveObjeto = armazenamentoObjetos.salvar(conteudo, nomeArquivoOriginal, mimeType);
        Anexo anexo = new Anexo(
                mensagemId, mimeType, conteudo.length, calcularChecksum(conteudo), chaveObjeto, nomeArquivoOriginal);
        return anexoRepository.save(anexo);
    }

    @Transactional(readOnly = true)
    public List<Mensagem> listarPorConversa(UUID conversaId) {
        return mensagemRepository.findByConversaIdOrderByEnviadaEmAsc(conversaId);
    }

    @Transactional(readOnly = true)
    public List<Anexo> listarAnexosDaMensagem(UUID mensagemId) {
        return anexoRepository.findByMensagemId(mensagemId);
    }

    private static String calcularChecksum(byte[] conteudo) {
        try {
            MessageDigest digestor = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digestor.digest(conteudo));
        } catch (NoSuchAlgorithmException excecao) {
            throw new DomainException("Algoritmo de checksum indisponível: " + excecao.getMessage());
        }
    }
}

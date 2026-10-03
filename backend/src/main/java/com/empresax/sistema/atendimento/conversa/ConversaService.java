package com.empresax.sistema.atendimento.conversa;

import com.empresax.sistema.cliente.ClienteRepository;
import com.empresax.sistema.common.domain.EntidadeNaoEncontradaException;
import com.empresax.sistema.usuario.Usuario;
import com.empresax.sistema.usuario.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ConversaService {

    private final ConversaRepository conversaRepository;
    private final ClienteRepository clienteRepository;
    private final UsuarioRepository usuarioRepository;

    public ConversaService(
            ConversaRepository conversaRepository,
            ClienteRepository clienteRepository,
            UsuarioRepository usuarioRepository
    ) {
        this.conversaRepository = conversaRepository;
        this.clienteRepository = clienteRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional
    public Conversa obterOuCriarAberta(String telefoneWhatsapp) {
        return conversaRepository.findByTelefoneWhatsappAndStatus(telefoneWhatsapp, StatusConversa.ABERTA)
                .orElseGet(() -> criar(telefoneWhatsapp));
    }

    private Conversa criar(String telefoneWhatsapp) {
        UUID clienteId = clienteRepository.findByTelefoneWhatsapp(telefoneWhatsapp)
                .map(cliente -> cliente.id())
                .orElse(null);
        return conversaRepository.save(new Conversa(telefoneWhatsapp, clienteId));
    }

    @Transactional(readOnly = true)
    public List<Conversa> listarAbertas() {
        return conversaRepository.findByStatus(StatusConversa.ABERTA);
    }

    @Transactional(readOnly = true)
    public Conversa buscarPorId(UUID id) {
        return conversaRepository.findById(id)
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Conversa não encontrada: " + id));
    }

    @Transactional
    public Conversa atribuirAtendente(UUID id, UUID atendenteId) {
        Conversa conversa = buscarPorId(id);
        conversa.atribuirAtendente(atendenteId);
        return conversa;
    }

    /** O usuário logado assume a conversa (o bot para de responder). */
    @Transactional
    public Conversa assumir(UUID id, String emailUsuario) {
        Usuario usuario = usuarioRepository.findByEmail(emailUsuario)
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Usuário não encontrado: " + emailUsuario));
        return atribuirAtendente(id, usuario.id());
    }

    @Transactional
    public Conversa devolverAoBot(UUID id) {
        Conversa conversa = buscarPorId(id);
        conversa.devolverAoBot();
        return conversa;
    }

    /**
     * Transação própria de propósito: é chamado quando a resposta do bot falhou (e a transação dela
     * foi desfeita) — a ida pra fila humana precisa persistir mesmo assim.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void transferirParaAtendente(UUID id, String motivo) {
        buscarPorId(id).transferirParaAtendente(motivo);
    }

    @Transactional
    public Conversa encerrar(UUID id) {
        Conversa conversa = buscarPorId(id);
        conversa.encerrar();
        return conversa;
    }
}

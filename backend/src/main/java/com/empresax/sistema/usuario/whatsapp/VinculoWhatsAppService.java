package com.empresax.sistema.usuario.whatsapp;

import com.empresax.sistema.atendimento.whatsapp.ConfiguracaoWhatsApp;
import com.empresax.sistema.atendimento.whatsapp.WhatsAppGateway;
import org.springframework.beans.factory.annotation.Qualifier;
import com.empresax.sistema.common.domain.DomainException;
import com.empresax.sistema.common.domain.EntidadeNaoEncontradaException;
import com.empresax.sistema.usuario.Usuario;
import com.empresax.sistema.usuario.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Optional;

/**
 * Liga o WhatsApp de um funcionário ao assistente do gestor: envia um código para o número e só
 * vincula quando a pessoa digita esse código no painel (prova de que o número é dela).
 */
@Service
public class VinculoWhatsAppService {

    private static final int LIMITE_CODIGO = 1_000_000;
    private static final String FORMATO_CODIGO = "%06d";
    private static final String MENSAGEM_CODIGO =
            "Código para ativar o assistente da Empresa X neste WhatsApp: %s%n"
                    + "Ele vale por 10 minutos. Se não foi você que pediu, ignore esta mensagem.";

    private final UsuarioRepository usuarioRepository;
    private final VerificacaoWhatsAppRepository verificacaoRepository;
    private final WhatsAppGateway whatsAppGateway;
    private final SecureRandom geradorAleatorio = new SecureRandom();

    public VinculoWhatsAppService(
            UsuarioRepository usuarioRepository,
            VerificacaoWhatsAppRepository verificacaoRepository,
            @Qualifier(ConfiguracaoWhatsApp.ASSISTENTE) WhatsAppGateway whatsAppGateway
    ) {
        this.usuarioRepository = usuarioRepository;
        this.verificacaoRepository = verificacaoRepository;
        this.whatsAppGateway = whatsAppGateway;
    }

    @Transactional
    public void enviarCodigo(String email, String telefoneDigitado) {
        Usuario usuario = buscar(email);
        if (!usuario.podeUsarAssistentePeloWhatsApp()) {
            throw new DomainException("Seu perfil de acesso não inclui o assistente");
        }
        TelefoneWhatsApp telefone = TelefoneWhatsApp.de(telefoneDigitado);
        if (usuarioRepository.existsByTelefoneWhatsappAndIdNot(telefone.numero(), usuario.id())) {
            throw new DomainException("Este número já está vinculado a outro funcionário");
        }

        Instant agora = Instant.now();
        verificacaoRepository.findById(usuario.id()).ifPresent(anterior -> {
            if (!anterior.permiteNovoEnvio(agora)) {
                throw new DomainException("Aguarde um minuto antes de pedir outro código");
            }
        });

        String codigo = String.format(FORMATO_CODIGO, geradorAleatorio.nextInt(LIMITE_CODIGO));
        verificacaoRepository.save(new VerificacaoWhatsApp(usuario.id(), telefone, codigo, agora));
        // Falha no envio desfaz a verificação (exceção de integração → rollback → 502 amigável).
        whatsAppGateway.enviarTexto(telefone.numero(), String.format(MENSAGEM_CODIGO, codigo));
    }

    /** Sem rollback em erro de domínio: a tentativa errada precisa ficar contada. */
    @Transactional(noRollbackFor = DomainException.class)
    public Usuario confirmar(String email, String codigo) {
        Usuario usuario = buscar(email);
        VerificacaoWhatsApp verificacao = verificacaoRepository.findById(usuario.id())
                .orElseThrow(() -> new DomainException("Nenhum código pendente. Peça um código primeiro."));

        TelefoneWhatsApp telefone = verificacao.confirmar(codigo, Instant.now());
        usuario.vincularWhatsApp(telefone);
        verificacaoRepository.delete(verificacao);
        return usuario;
    }

    @Transactional
    public Usuario desvincular(String email) {
        Usuario usuario = buscar(email);
        usuario.desvincularWhatsApp();
        verificacaoRepository.deleteById(usuario.id());
        return usuario;
    }

    @Transactional(readOnly = true)
    public Optional<Usuario> buscarVinculado(String telefone) {
        return usuarioRepository.findComPermissoesByTelefoneWhatsapp(telefone);
    }

    private Usuario buscar(String email) {
        return usuarioRepository.findComPermissoesByEmail(email)
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Usuário não encontrado: " + email));
    }
}

package com.empresax.sistema.usuario;

import com.empresax.sistema.acesso.Cargo;
import com.empresax.sistema.acesso.CargoService;
import com.empresax.sistema.acesso.Permissao;
import com.empresax.sistema.common.domain.AcessoNegadoException;
import com.empresax.sistema.common.domain.DomainException;
import com.empresax.sistema.common.domain.EntidadeNaoEncontradaException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Gestão de usuários com regras contra escalada de privilégio:
 * - só quem tem acesso irrestrito cria ou altera outro usuário irrestrito;
 * - ninguém atribui um cargo com permissões que não tem;
 * - ninguém altera o próprio cargo nem se desativa.
 */
@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final CargoService cargoService;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository, CargoService cargoService, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.cargoService = cargoService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public Usuario buscarComPermissoes(String email) {
        return usuarioRepository.findComPermissoesByEmail(email)
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Usuário não encontrado: " + email));
    }

    /**
     * Confere e-mail e senha na hora (ex.: o gerente autorizando um desconto no caixa). Mensagem
     * única para e-mail inexistente, senha errada ou funcionário desativado: não ajuda quem tenta adivinhar.
     */
    @Transactional(readOnly = true)
    public Usuario conferirCredenciais(String email, String senha) {
        return usuarioRepository.findComPermissoesByEmail(email == null ? "" : email.trim())
                .filter(Usuario::ativo)
                .filter(usuario -> senha != null && passwordEncoder.matches(senha, usuario.senhaCriptografada()))
                .orElseThrow(() -> new DomainException("E-mail ou senha de quem autoriza estão incorretos"));
    }

    /** Nome para exibir de quem registrou algo (vendas guardam o e-mail, que é a identidade). */
    @Transactional(readOnly = true)
    public Map<String, String> nomesPorEmail(Collection<String> emails) {
        if (emails.isEmpty()) {
            return Map.of();
        }
        return usuarioRepository.findByEmailIn(emails).stream()
                .collect(Collectors.toUnmodifiableMap(Usuario::email, Usuario::nome));
    }

    @Transactional(readOnly = true)
    public List<Usuario> listarTodos() {
        return usuarioRepository.findAllByOrderByNomeAsc();
    }

    /** Funcionários ativos que podem vender no caixa (o caixa é aberto em nome deles). */
    @Transactional(readOnly = true)
    public List<Usuario> listarQuemPodeVender() {
        return usuarioRepository.findAllByOrderByNomeAsc().stream()
                .filter(usuario -> usuario.ativo() && usuario.possui(Permissao.PDV_VENDER))
                .toList();
    }

    @Transactional(readOnly = true)
    public Usuario buscarOperadorDeCaixa(UUID usuarioId) {
        Usuario usuario = usuarioRepository.findComPermissoesById(usuarioId)
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Funcionário não encontrado: " + usuarioId));
        if (!usuario.ativo() || !usuario.possui(Permissao.PDV_VENDER)) {
            throw new DomainException("Este funcionário não pode vender no caixa");
        }
        return usuario;
    }

    @Transactional
    public Usuario cadastrar(String emailAtor, NovoUsuario novo) {
        Usuario ator = buscarComPermissoes(emailAtor);
        usuarioRepository.findByEmail(novo.email()).ifPresent(existente -> {
            throw new DomainException("Já existe um usuário cadastrado com este e-mail");
        });

        String senhaCriptografada = passwordEncoder.encode(novo.senha());
        if (novo.acessoIrrestrito()) {
            garantirIrrestrito(ator, "Só quem tem acesso irrestrito pode criar outro usuário irrestrito");
            return usuarioRepository.save(Usuario.comAcessoIrrestrito(novo.nome(), novo.email(), senhaCriptografada));
        }

        Cargo cargo = cargoConcedivel(ator, novo.cargoId());
        return usuarioRepository.save(Usuario.comCargo(novo.nome(), novo.email(), senhaCriptografada, cargo));
    }

    @Transactional
    public Usuario trocarCargo(String emailAtor, UUID usuarioId, UUID cargoId) {
        Usuario ator = buscarComPermissoes(emailAtor);
        Usuario alvo = buscarAlvo(ator, usuarioId, "Você não pode alterar o próprio perfil de acesso");
        alvo.trocarCargo(cargoConcedivel(ator, cargoId));
        return alvo;
    }

    @Transactional
    public Usuario desativar(String emailAtor, UUID usuarioId) {
        Usuario ator = buscarComPermissoes(emailAtor);
        Usuario alvo = buscarAlvo(ator, usuarioId, "Você não pode desativar a si mesmo");
        alvo.desativar();
        return alvo;
    }

    @Transactional
    public Usuario ativar(String emailAtor, UUID usuarioId) {
        Usuario ator = buscarComPermissoes(emailAtor);
        Usuario alvo = buscarAlvo(ator, usuarioId, "Você não pode alterar a si mesmo");
        alvo.ativar();
        return alvo;
    }

    private Usuario buscarAlvo(Usuario ator, UUID usuarioId, String mensagemSeForOProprio) {
        Usuario alvo = usuarioRepository.findComPermissoesById(usuarioId)
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Usuário não encontrado: " + usuarioId));
        if (alvo.id().equals(ator.id())) {
            throw new AcessoNegadoException(mensagemSeForOProprio);
        }
        if (alvo.acessoIrrestrito()) {
            garantirIrrestrito(ator, "Só quem tem acesso irrestrito pode alterar um usuário irrestrito");
        }
        return alvo;
    }

    private Cargo cargoConcedivel(Usuario ator, UUID cargoId) {
        if (cargoId == null) {
            throw new DomainException("Escolha um perfil de acesso para o funcionário");
        }
        Cargo cargo = cargoService.buscarPorId(cargoId);
        if (!ator.podeConceder(cargo.permissoes())) {
            throw new AcessoNegadoException("Você não pode atribuir um perfil de acesso com permissões que você mesmo não tem");
        }
        return cargo;
    }

    private static void garantirIrrestrito(Usuario ator, String mensagem) {
        if (!ator.acessoIrrestrito()) {
            throw new AcessoNegadoException(mensagem);
        }
    }
}

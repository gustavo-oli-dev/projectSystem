package com.empresax.sistema.usuario.seguranca;

import com.empresax.sistema.usuario.Usuario;
import com.empresax.sistema.usuario.UsuarioRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * As autoridades do Spring Security são as permissões do usuário (as do cargo, ou todas para acesso
 * irrestrito). Carregadas do banco a cada requisição autenticada: mudar o cargo de alguém vale na
 * hora, sem esperar o token expirar.
 */
@Service
public class UsuarioDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    public UsuarioDetailsService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) {
        Usuario usuario = usuarioRepository.findComPermissoesByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado: " + email));

        List<SimpleGrantedAuthority> autoridades = usuario.permissoes().stream()
                .map(permissao -> new SimpleGrantedAuthority(permissao.name()))
                .toList();

        return User.builder()
                .username(usuario.email())
                .password(usuario.senhaCriptografada())
                .authorities(autoridades)
                .disabled(!usuario.ativo())
                .build();
    }
}

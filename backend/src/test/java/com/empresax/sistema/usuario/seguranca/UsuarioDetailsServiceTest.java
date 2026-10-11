package com.empresax.sistema.usuario.seguranca;

import com.empresax.sistema.acesso.Cargo;
import com.empresax.sistema.acesso.Permissao;
import com.empresax.sistema.usuario.Usuario;
import com.empresax.sistema.usuario.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Regressão de um defeito achado no primeiro deploy (D50): login com o e-mail numa letra diferente
 * de como foi cadastrado (ex.: "Admin@X.com" digitado, "admin@x.com" salvo) dava "usuário não
 * encontrado" mesmo com a senha certa, porque a consulta não normalizava o que a pessoa digitou.
 */
class UsuarioDetailsServiceTest {

    private static final Cargo GERENTE = new Cargo("Gerente", null, Set.of(Permissao.PDV_AUTORIZAR));

    private final UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
    private final UsuarioDetailsService servico = new UsuarioDetailsService(usuarioRepository);

    @Test
    void achaOUsuarioMesmoDigitandoOEmailComMaiusculaEEspaco() {
        Usuario usuario = Usuario.comCargo("Dono", "dono@empresax.com", "hash-da-senha", GERENTE);
        when(usuarioRepository.findComPermissoesByEmail(eq("dono@empresax.com"))).thenReturn(Optional.of(usuario));

        UserDetails detalhes = servico.loadUserByUsername("  Dono@EmpresaX.com  ");

        assertThat(detalhes.getUsername()).isEqualTo("dono@empresax.com");
        assertThat(detalhes.getPassword()).isEqualTo("hash-da-senha");
    }

    @Test
    void naoAchandoLancaUsernameNotFound() {
        when(usuarioRepository.findComPermissoesByEmail(eq("ninguem@empresax.com"))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servico.loadUserByUsername("ninguem@empresax.com"))
                .isInstanceOf(UsernameNotFoundException.class);
    }
}

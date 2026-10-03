package com.empresax.sistema.usuario;

import com.empresax.sistema.acesso.Cargo;
import com.empresax.sistema.acesso.Permissao;
import com.empresax.sistema.common.domain.DomainException;
import com.empresax.sistema.usuario.whatsapp.TelefoneWhatsApp;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UsuarioTest {

    private static final Cargo ATENDIMENTO = new Cargo(
            "Atendimento", "Call center", Set.of(Permissao.CONVERSAS_VER, Permissao.CLIENTES_VER));
    private static final Cargo GERENTE = new Cargo(
            "Gerente", null, Set.of(Permissao.PEDIDOS_VER, Permissao.PEDIDOS_GERENCIAR));

    private static Usuario atendente() {
        return Usuario.comCargo("Maria Silva", "maria@empresax.com", "hash", ATENDIMENTO);
    }

    @Test
    void criaUsuarioAtivoPorPadrao() {
        assertThat(atendente().ativo()).isTrue();
    }

    @Test
    void rejeitaEmailInvalido() {
        assertThatThrownBy(() -> Usuario.comCargo("Maria Silva", "email-invalido", "hash", ATENDIMENTO))
                .isInstanceOf(DomainException.class);
    }

    @Test
    void rejeitaNomeEmBranco() {
        assertThatThrownBy(() -> Usuario.comCargo(" ", "maria@empresax.com", "hash", ATENDIMENTO))
                .isInstanceOf(DomainException.class);
    }

    @Test
    void rejeitaUsuarioSemCargoESemAcessoIrrestrito() {
        assertThatThrownBy(() -> Usuario.comCargo("Maria Silva", "maria@empresax.com", "hash", null))
                .isInstanceOf(DomainException.class);
    }

    @Test
    void desativarTornaUsuarioInativo() {
        Usuario usuario = atendente();

        usuario.desativar();

        assertThat(usuario.ativo()).isFalse();
    }

    @Test
    void usuarioComCargoTemExatamenteAsPermissoesDoCargo() {
        Usuario usuario = atendente();

        assertThat(usuario.permissoes()).containsExactlyInAnyOrder(Permissao.CONVERSAS_VER, Permissao.CLIENTES_VER);
        assertThat(usuario.possui(Permissao.FATURAMENTO_VER)).isFalse();
    }

    @Test
    void acessoIrrestritoPossuiTodasAsPermissoes() {
        Usuario dono = Usuario.comAcessoIrrestrito("Dono", "dono@empresax.com", "hash");

        assertThat(dono.permissoes()).containsExactlyInAnyOrderElementsOf(EnumSet.allOf(Permissao.class));
        assertThat(dono.possui(Permissao.FISCAL_GERENCIAR)).isTrue();
    }

    @Test
    void usuarioSoPodeConcederPermissoesQueEleMesmoTem() {
        Usuario usuario = atendente();

        assertThat(usuario.podeConceder(Set.of(Permissao.CONVERSAS_VER))).isTrue();
        assertThat(usuario.podeConceder(Set.of(Permissao.CONVERSAS_VER, Permissao.FATURAMENTO_VER))).isFalse();
    }

    @Test
    void acessoIrrestritoPodeConcederQualquerPermissao() {
        Usuario dono = Usuario.comAcessoIrrestrito("Dono", "dono@empresax.com", "hash");

        assertThat(dono.podeConceder(EnumSet.allOf(Permissao.class))).isTrue();
    }

    @Test
    void cargoSemAssistenteNaoPodeVincularWhatsApp() {
        assertThatThrownBy(() -> atendente().vincularWhatsApp(TelefoneWhatsApp.de("85988887777")))
                .isInstanceOf(DomainException.class);
    }

    @Test
    void comAssistenteVinculaEDesvinculaOWhatsApp() {
        Usuario dono = Usuario.comAcessoIrrestrito("Dono", "dono@empresax.com", "hash");

        dono.vincularWhatsApp(TelefoneWhatsApp.de("85988887777"));
        assertThat(dono.telefoneWhatsapp()).contains("5585988887777");

        dono.desvincularWhatsApp();
        assertThat(dono.telefoneWhatsapp()).isEmpty();
    }

    @Test
    void desativadoNaoUsaOAssistentePeloWhatsAppMesmoVinculado() {
        Usuario dono = Usuario.comAcessoIrrestrito("Dono", "dono@empresax.com", "hash");
        dono.vincularWhatsApp(TelefoneWhatsApp.de("85988887777"));

        dono.desativar();

        assertThat(dono.podeUsarAssistentePeloWhatsApp()).isFalse();
    }

    @Test
    void trocarCargoSubstituiAsPermissoesERemoveAcessoIrrestrito() {
        Usuario usuario = Usuario.comAcessoIrrestrito("Ex-sócio", "socio@empresax.com", "hash");

        usuario.trocarCargo(GERENTE);

        assertThat(usuario.acessoIrrestrito()).isFalse();
        assertThat(usuario.permissoes()).containsExactlyInAnyOrder(Permissao.PEDIDOS_VER, Permissao.PEDIDOS_GERENCIAR);
    }
}

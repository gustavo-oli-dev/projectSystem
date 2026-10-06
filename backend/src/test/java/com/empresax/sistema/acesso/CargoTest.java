package com.empresax.sistema.acesso;

import com.empresax.sistema.common.domain.DomainException;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CargoTest {

    @Test
    void criaCargoComAsPermissoesInformadas() {
        Cargo cargo = new Cargo("  Call center  ", "Atende o WhatsApp", Set.of(Permissao.CONVERSAS_VER));

        assertThat(cargo.nome()).isEqualTo("Call center");
        assertThat(cargo.possui(Permissao.CONVERSAS_VER)).isTrue();
        assertThat(cargo.possui(Permissao.FATURAMENTO_VER)).isFalse();
    }

    @Test
    void rejeitaCargoSemPermissoes() {
        assertThatThrownBy(() -> new Cargo("Vazio", null, Set.of()))
                .isInstanceOf(DomainException.class);
    }

    @Test
    void rejeitaNomeEmBranco() {
        assertThatThrownBy(() -> new Cargo(" ", null, Set.of(Permissao.PAINEL_OPERACAO)))
                .isInstanceOf(DomainException.class);
    }

    @Test
    void rejeitaNomeMaiorQueOitentaCaracteres() {
        assertThatThrownBy(() -> new Cargo("x".repeat(81), null, Set.of(Permissao.PAINEL_OPERACAO)))
                .isInstanceOf(DomainException.class);
    }

    @Test
    void rejeitaDescricaoMaiorQueDuzentosECinquentaECincoCaracteres() {
        assertThatThrownBy(() -> new Cargo("Gerente", "x".repeat(256), Set.of(Permissao.PAINEL_OPERACAO)))
                .isInstanceOf(DomainException.class);
    }

    @Test
    void redefinirSubstituiNomeEPermissoes() {
        Cargo cargo = new Cargo("Gerente", null, Set.of(Permissao.PEDIDOS_VER));

        cargo.redefinir("Gerente comercial", "Sem faturamento", Set.of(Permissao.CLIENTES_VER));

        assertThat(cargo.nome()).isEqualTo("Gerente comercial");
        assertThat(cargo.permissoes()).containsExactly(Permissao.CLIENTES_VER);
    }

    @Test
    void redefinirSemPermissoesMantemOEstadoAnterior() {
        Cargo cargo = new Cargo("Gerente", null, Set.of(Permissao.PEDIDOS_VER));

        assertThatThrownBy(() -> cargo.redefinir("Gerente", null, Set.of()))
                .isInstanceOf(DomainException.class);
        assertThat(cargo.permissoes()).containsExactly(Permissao.PEDIDOS_VER);
    }

    @Test
    void permissoesRetornadasNaoPodemSerAlteradasPorFora() {
        Cargo cargo = new Cargo("Gerente", null, Set.of(Permissao.PEDIDOS_VER));

        assertThatThrownBy(() -> cargo.permissoes().add(Permissao.FATURAMENTO_VER))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}

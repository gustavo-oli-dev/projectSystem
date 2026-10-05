package com.empresax.sistema.pdv.autorizacao;

import com.empresax.sistema.acesso.Permissao;
import com.empresax.sistema.common.domain.DomainException;
import com.empresax.sistema.usuario.Usuario;
import com.empresax.sistema.usuario.UsuarioService;
import com.empresax.sistema.usuario.seguranca.JwtService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AutorizacaoCaixaServiceTest {

    private static final String SEGREDO = "segredo-de-teste-com-pelo-menos-32-bytes-para-hmac-sha256";
    private static final String OPERADOR = "caixa1@x.com";
    private static final String GERENTE = "gerente@x.com";

    private final UsuarioService usuarioService = mock(UsuarioService.class);
    private final AutorizacaoCaixaService servico = new AutorizacaoCaixaService(usuarioService, SEGREDO);

    @Test
    void gerenteComPermissaoAutorizaEOTokenValeParaAAcaoEOOperador() {
        gerente(true);

        String token = servico.autorizar(GERENTE, "senha", AcaoAutorizada.DESCONTO, OPERADOR).token();

        assertThat(servico.validar(token, AcaoAutorizada.DESCONTO, OPERADOR)).isEqualTo(GERENTE);
    }

    @Test
    void tokenNaoServeParaOutraAcaoNemParaOutroOperador() {
        gerente(true);
        String token = servico.autorizar(GERENTE, "senha", AcaoAutorizada.DESCONTO, OPERADOR).token();

        assertThatThrownBy(() -> servico.validar(token, AcaoAutorizada.CANCELAR_ITEM, OPERADOR))
                .isInstanceOf(DomainException.class).hasMessageContaining("inválida ou vencida");
        assertThatThrownBy(() -> servico.validar(token, AcaoAutorizada.DESCONTO, "outro-caixa@x.com"))
                .hasMessageContaining("inválida ou vencida");
    }

    @Test
    void quemNaoTemPermissaoNaoAutoriza() {
        gerente(false);

        assertThatThrownBy(() -> servico.autorizar(GERENTE, "senha", AcaoAutorizada.DESCONTO, OPERADOR))
                .hasMessageContaining("não tem permissão para autorizar");
    }

    @Test
    void tokenAdulteradoOuVazioEhRecusado() {
        assertThatThrownBy(() -> servico.validar("abc.def.ghi", AcaoAutorizada.DESCONTO, OPERADOR)).hasMessageContaining("inválida");
        assertThatThrownBy(() -> servico.validar(" ", AcaoAutorizada.DESCONTO, OPERADOR)).hasMessageContaining("autorização de um gerente");
    }

    @Test
    void tokenDeAutorizacaoNaoServeComoLoginDoGerente() {
        gerente(true);
        String token = servico.autorizar(GERENTE, "senha", AcaoAutorizada.DESCONTO, OPERADOR).token();
        JwtService login = new JwtService(SEGREDO, 60);

        assertThatThrownBy(() -> login.extrairEmail(token)).isInstanceOf(RuntimeException.class);
    }

    private void gerente(boolean podeAutorizar) {
        Usuario usuario = mock(Usuario.class);
        when(usuario.email()).thenReturn(GERENTE);
        when(usuario.nome()).thenReturn("Gerente");
        when(usuario.possui(Permissao.PDV_AUTORIZAR)).thenReturn(podeAutorizar);
        when(usuarioService.conferirCredenciais(GERENTE, "senha")).thenReturn(usuario);
    }
}

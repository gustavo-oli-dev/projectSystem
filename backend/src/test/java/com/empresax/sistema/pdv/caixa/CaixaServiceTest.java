package com.empresax.sistema.pdv.caixa;

import com.empresax.sistema.common.domain.DomainException;
import com.empresax.sistema.pdv.caixa.CaixaService.AberturaDeCaixa;
import com.empresax.sistema.usuario.Usuario;
import com.empresax.sistema.usuario.UsuarioService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CaixaServiceTest {

    private static final String GERENTE = "gerente@empresax.com";
    private static final ContagemCedulas FUNDO = new ContagemCedulas(Map.of(Cedula.NOTA_10, 5));

    private final SessaoCaixaRepository sessaoRepository = mock(SessaoCaixaRepository.class);
    private final PontoCaixaRepository pontoRepository = mock(PontoCaixaRepository.class);
    private final UsuarioService usuarioService = mock(UsuarioService.class);
    private final CaixaService caixaService = new CaixaService(
            sessaoRepository, pontoRepository, mock(CedulaFundoTrocoPadraoRepository.class),
            mock(VendasDoCaixaConsulta.class), usuarioService);

    @Test
    void abreVariosCaixasCadaUmComOSeuOperadorEOMesmoFundo() {
        UUID caixa1 = ponto(1);
        UUID caixa2 = ponto(2);
        UUID ana = operador("ana@empresax.com", "Ana");
        UUID bia = operador("bia@empresax.com", "Bia");
        nenhumCaixaAberto();
        when(sessaoRepository.saveAndFlush(any(SessaoCaixa.class))).thenAnswer(chamada -> chamada.getArgument(0));

        List<SessaoCaixa> abertos = caixaService.abrir(
                List.of(new AberturaDeCaixa(caixa1, ana), new AberturaDeCaixa(caixa2, bia)), FUNDO, GERENTE);

        assertThat(abertos).extracting(SessaoCaixa::operador).containsExactly("ana@empresax.com", "bia@empresax.com");
        assertThat(abertos).extracting(sessao -> sessao.pontoCaixaId().orElseThrow()).containsExactly(caixa1, caixa2);
        assertThat(abertos).allSatisfy(caixa -> {
            assertThat(caixa.fundoInicial()).isEqualTo(FUNDO.total());
            assertThat(caixa.abertaPor()).isEqualTo(GERENTE);
        });
    }

    @Test
    void caixaFisicoJaAbertoImpedeAAberturaEDizQual() {
        UUID caixa1 = ponto(1);
        UUID ana = operador("ana@empresax.com", "Ana");
        when(sessaoRepository.findByPontoCaixaIdAndStatus(caixa1, StatusSessaoCaixa.ABERTA))
                .thenReturn(Optional.of(SessaoCaixa.abrir("bia@empresax.com", caixa1, FUNDO, GERENTE)));

        assertThatThrownBy(() -> caixaService.abrir(List.of(new AberturaDeCaixa(caixa1, ana)), FUNDO, GERENTE))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("Caixa 01 já está aberto");
        verify(sessaoRepository, never()).saveAndFlush(any());
    }

    @Test
    void operadorQueJaEstaEmOutroCaixaNaoAbreMaisUm() {
        UUID caixa2 = ponto(2);
        UUID ana = operador("ana@empresax.com", "Ana");
        when(sessaoRepository.findByPontoCaixaIdAndStatus(caixa2, StatusSessaoCaixa.ABERTA)).thenReturn(Optional.empty());
        when(sessaoRepository.findByOperadorAndStatus("ana@empresax.com", StatusSessaoCaixa.ABERTA))
                .thenReturn(Optional.of(SessaoCaixa.abrir("ana@empresax.com", UUID.randomUUID(), FUNDO, GERENTE)));

        assertThatThrownBy(() -> caixaService.abrir(List.of(new AberturaDeCaixa(caixa2, ana)), FUNDO, GERENTE))
                .hasMessageContaining("Ana já está em um caixa aberto");
    }

    @Test
    void mesmoCaixaOuMesmoOperadorRepetidoNaAberturaEhRecusado() {
        UUID caixa = UUID.randomUUID();
        UUID ana = UUID.randomUUID();

        assertThatThrownBy(() -> caixaService.abrir(List.of(), FUNDO, GERENTE)).hasMessageContaining("ao menos um caixa");
        assertThatThrownBy(() -> caixaService.abrir(
                List.of(new AberturaDeCaixa(caixa, ana), new AberturaDeCaixa(caixa, UUID.randomUUID())), FUNDO, GERENTE))
                .hasMessageContaining("mesmo caixa");
        assertThatThrownBy(() -> caixaService.abrir(
                List.of(new AberturaDeCaixa(caixa, ana), new AberturaDeCaixa(UUID.randomUUID(), ana)), FUNDO, GERENTE))
                .hasMessageContaining("mesmo operador");
    }

    @Test
    void numeroDeCaixaRepetidoEhRecusado() {
        when(pontoRepository.existsByNumero(3)).thenReturn(true);

        assertThatThrownBy(() -> caixaService.cadastrarPonto(3)).hasMessageContaining("Já existe o caixa número 3");
    }

    private void nenhumCaixaAberto() {
        when(sessaoRepository.findByOperadorAndStatus(anyString(), eq(StatusSessaoCaixa.ABERTA))).thenReturn(Optional.empty());
        when(sessaoRepository.findByPontoCaixaIdAndStatus(any(UUID.class), eq(StatusSessaoCaixa.ABERTA))).thenReturn(Optional.empty());
    }

    private UUID ponto(int numero) {
        PontoCaixa ponto = mock(PontoCaixa.class);
        UUID id = UUID.randomUUID();
        when(ponto.id()).thenReturn(id);
        when(ponto.ativo()).thenReturn(true);
        when(ponto.nome()).thenReturn("Caixa %02d".formatted(numero));
        when(pontoRepository.findById(id)).thenReturn(Optional.of(ponto));
        return id;
    }

    private UUID operador(String email, String nome) {
        UUID id = UUID.randomUUID();
        Usuario usuario = mock(Usuario.class);
        when(usuario.email()).thenReturn(email);
        when(usuario.nome()).thenReturn(nome);
        when(usuarioService.buscarOperadorDeCaixa(id)).thenReturn(usuario);
        return id;
    }
}

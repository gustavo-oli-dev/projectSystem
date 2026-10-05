package com.empresax.sistema.pdv.autorizacao;

import com.empresax.sistema.acesso.Permissao;
import com.empresax.sistema.common.domain.DomainException;
import com.empresax.sistema.usuario.Usuario;
import com.empresax.sistema.usuario.UsuarioService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

/**
 * Autorização do gerente no caixa (D35). O gerente digita e-mail e senha na hora; se ele tiver
 * PDV_AUTORIZAR, sai um token curto (5 minutos) preso à ação e ao operador que pediu. A venda ou
 * o cancelamento mandam o token — a senha nunca fica guardada no navegador.
 *
 * A chave de assinatura é DERIVADA do segredo do login, mas diferente dele: um token de
 * autorização nunca serve para entrar no sistema como o gerente.
 */
@Service
public class AutorizacaoCaixaService {

    private static final Duration VALIDADE = Duration.ofMinutes(5);
    private static final String PROPOSITO_DA_CHAVE = "|autorizacao-caixa";
    private static final String CLAIM_ACAO = "acao";
    private static final String CLAIM_OPERADOR = "operador";
    private static final String AUTORIZACAO_INVALIDA = "Autorização do gerente inválida ou vencida — peça de novo";

    private final UsuarioService usuarioService;
    private final SecretKey chave;

    public AutorizacaoCaixaService(UsuarioService usuarioService, @Value("${seguranca.jwt.segredo}") String segredo) {
        this.usuarioService = usuarioService;
        this.chave = Keys.hmacShaKeyFor(sha256(segredo + PROPOSITO_DA_CHAVE));
    }

    public AutorizacaoEmitida autorizar(String emailGerente, String senha, AcaoAutorizada acao, String operador) {
        if (acao == null) {
            throw new DomainException("Informe o que está sendo autorizado");
        }
        Usuario gerente = usuarioService.conferirCredenciais(emailGerente, senha);
        if (!gerente.possui(Permissao.PDV_AUTORIZAR)) {
            throw new DomainException(gerente.nome() + " não tem permissão para autorizar no caixa");
        }
        Instant expira = Instant.now().plus(VALIDADE);
        String token = Jwts.builder()
                .subject(gerente.email())
                .claim(CLAIM_ACAO, acao.name())
                .claim(CLAIM_OPERADOR, operador)
                .expiration(Date.from(expira))
                .signWith(chave)
                .compact();
        return new AutorizacaoEmitida(token, gerente.email(), gerente.nome(), expira);
    }

    /** Confere o token para esta ação e este operador; devolve o e-mail de quem autorizou. */
    public String validar(String token, AcaoAutorizada acao, String operador) {
        if (token == null || token.isBlank()) {
            throw new DomainException("Esta ação precisa da autorização de um gerente");
        }
        try {
            Claims claims = Jwts.parser().verifyWith(chave).build().parseSignedClaims(token).getPayload();
            boolean mesmaAcao = acao.name().equals(claims.get(CLAIM_ACAO, String.class));
            boolean mesmoOperador = operador != null && operador.equals(claims.get(CLAIM_OPERADOR, String.class));
            if (!mesmaAcao || !mesmoOperador) {
                throw new DomainException(AUTORIZACAO_INVALIDA);
            }
            return claims.getSubject();
        } catch (JwtException | IllegalArgumentException invalido) {
            throw new DomainException(AUTORIZACAO_INVALIDA);
        }
    }

    private static byte[] sha256(String texto) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(texto.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException impossivel) {
            throw new IllegalStateException("SHA-256 indisponível na JVM", impossivel);
        }
    }

    public record AutorizacaoEmitida(String token, String autorizadoPor, String autorizadoPorNome, Instant expiraEm) {
    }
}

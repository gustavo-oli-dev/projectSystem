package com.empresax.sistema.usuario.seguranca;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {

    private final SecretKey chaveAssinatura;
    private final Duration tempoExpiracao;

    public JwtService(
            @Value("${seguranca.jwt.segredo}") String segredo,
            @Value("${seguranca.jwt.expiracaoMinutos}") long expiracaoMinutos
    ) {
        this.chaveAssinatura = Keys.hmacShaKeyFor(segredo.getBytes());
        this.tempoExpiracao = Duration.ofMinutes(expiracaoMinutos);
    }

    /** O token só identifica o usuário; as permissões são lidas do banco a cada requisição. */
    public String gerarToken(String email) {
        Instant agora = Instant.now();
        return Jwts.builder()
                .subject(email)
                .issuedAt(Date.from(agora))
                .expiration(Date.from(agora.plus(tempoExpiracao)))
                .signWith(chaveAssinatura)
                .compact();
    }

    public String extrairEmail(String token) {
        return extrairClaims(token).getSubject();
    }

    public boolean tokenValido(String token, String email) {
        Claims claims = extrairClaims(token);
        boolean emailConfere = claims.getSubject().equals(email);
        boolean naoExpirado = claims.getExpiration().after(new Date());
        return emailConfere && naoExpirado;
    }

    private Claims extrairClaims(String token) {
        return Jwts.parser()
                .verifyWith(chaveAssinatura)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}

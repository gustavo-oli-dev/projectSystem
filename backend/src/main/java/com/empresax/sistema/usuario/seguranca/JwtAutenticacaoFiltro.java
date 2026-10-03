package com.empresax.sistema.usuario.seguranca;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class JwtAutenticacaoFiltro extends OncePerRequestFilter {

    private static final String PREFIXO_BEARER = "Bearer ";

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    public JwtAutenticacaoFiltro(JwtService jwtService, UserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String cabecalhoAutorizacao = request.getHeader("Authorization");
        if (cabecalhoAutorizacao == null || !cabecalhoAutorizacao.startsWith(PREFIXO_BEARER)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = cabecalhoAutorizacao.substring(PREFIXO_BEARER.length());
        String email = jwtService.extrairEmail(token);
        boolean aindaNaoAutenticado = SecurityContextHolder.getContext().getAuthentication() == null;

        if (email != null && aindaNaoAutenticado) {
            autenticar(request, token, email);
        }

        filterChain.doFilter(request, response);
    }

    private void autenticar(HttpServletRequest request, String token, String email) {
        UserDetails userDetails = userDetailsService.loadUserByUsername(email);
        // Funcionário desativado perde o acesso na hora, mesmo com token ainda dentro da validade.
        if (!userDetails.isEnabled() || !jwtService.tokenValido(token, email)) {
            return;
        }
        UsernamePasswordAuthenticationToken autenticacao =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        autenticacao.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(autenticacao);
    }
}

package com.empresax.sistema.config;

import com.empresax.sistema.usuario.seguranca.JwtAutenticacaoFiltro;
import com.empresax.sistema.usuario.seguranca.JwtService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    // Únicos endpoints públicos do sistema, conforme exigido pela regra "autenticação por padrão":
    // login (ainda não há token a apresentar) e a documentação da API.
    private static final String[] ENDPOINTS_PUBLICOS = {
            "/api/auth/login",
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            // O Mercado Pago não carrega nosso JWT; autenticidade garantida por assinatura HMAC
            // (ver AssinaturaMercadoPagoValidador), não por este filtro.
            "/api/webhooks/mercadopago",
            // A W-API não carrega nosso JWT nem cabeçalho customizado; autenticidade garantida por
            // token secreto na URL do webhook (ver WApiWebhookController), não por este filtro.
            "/api/webhooks/wapi",
            // Mesmo esquema, para o número interno do assistente (D17), com token próprio.
            "/api/webhooks/wapi-assistente"
    };

    private static final String[] FOTOS_PUBLICAS = {
            "/api/produtos/*/fotos/*"
    };

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuracao) throws Exception {
        return configuracao.getAuthenticationManager();
    }

    @Bean
    public JwtAutenticacaoFiltro jwtAutenticacaoFiltro(JwtService jwtService, UserDetailsService userDetailsService) {
        return new JwtAutenticacaoFiltro(jwtService, userDetailsService);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtAutenticacaoFiltro jwtAutenticacaoFiltro) throws Exception {
        http
                // CSRF protege sessão baseada em cookie; esta API é stateless (JWT em header
                // Authorization), então não há cookie de sessão para um CSRF explorar.
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sessao -> sessao.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(requisicoes -> requisicoes
                        .requestMatchers(ENDPOINTS_PUBLICOS).permitAll()
                        // Fotos dos produtos, só leitura (GET): a tag <img> do painel não manda o token.
                        // Mesmo caminho com outro método exige login.
                        .requestMatchers(HttpMethod.GET, FOTOS_PUBLICAS).permitAll()
                        .anyRequest().authenticated())
                // Sem login válido (token ausente/expirado, funcionário desativado) → 401, para o
                // painel voltar ao login. Logado sem a permissão → 403 (GlobalExceptionHandler).
                .exceptionHandling(excecoes -> excecoes
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .addFilterBefore(jwtAutenticacaoFiltro, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}

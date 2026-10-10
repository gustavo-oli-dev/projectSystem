package com.empresax.sistema.config;

import com.empresax.sistema.usuario.seguranca.JwtAutenticacaoFiltro;
import com.empresax.sistema.usuario.seguranca.JwtService;
import org.springframework.beans.factory.annotation.Value;
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
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    // Únicos endpoints públicos do sistema, conforme exigido pela regra "autenticação por padrão":
    // login (ainda não há token a apresentar), a documentação da API e o health check do host de
    // deploy (SaudeController — confirma só que o processo está vivo, nenhum dado do negócio).
    private static final String[] ENDPOINTS_PUBLICOS = {
            "/api/auth/login",
            "/api/saude",
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

    /**
     * Origens que podem chamar a API de outro domínio (ex.: o frontend publicado no Netlify,
     * separado do backend no Render). Lista separada por vírgula, vazia por padrão: quando o
     * frontend é servido pelo mesmo nginx que faz proxy para /api (Docker Compose local), é tudo
     * mesma origem e CORS nem entra em jogo.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${cors.origens-permitidas:}") String origensPermitidas
    ) {
        List<String> origens = Arrays.stream(origensPermitidas.split(","))
                .map(String::trim)
                .filter(origem -> !origem.isBlank())
                .toList();
        CorsConfiguration configuracao = new CorsConfiguration();
        configuracao.setAllowedOrigins(origens);
        configuracao.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuracao.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        // JWT vai no header Authorization, não em cookie — sem sessão para credentials trocar.
        configuracao.setAllowCredentials(false);
        UrlBasedCorsConfigurationSource fonte = new UrlBasedCorsConfigurationSource();
        fonte.registerCorsConfiguration("/**", configuracao);
        return fonte;
    }

    @Bean
    public SecurityFilterChain filterChain(
            HttpSecurity http, JwtAutenticacaoFiltro jwtAutenticacaoFiltro, CorsConfigurationSource corsConfigurationSource,
            @Value("${cors.origens-permitidas:}") String origensPermitidas
    ) throws Exception {
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
        // Só liga o filtro de CORS quando há de fato uma origem externa configurada (Netlify
        // chamando o Render). Ligado com a lista vazia, o filtro rejeitaria também as requisições
        // locais: o navegador manda o cabeçalho Origin mesmo em POST de mesma origem, e atravessando
        // o proxy do nginx local esse Origin nem sempre bate byte a byte com o que o backend vê
        // (porta ausente no Host repassado) — o pedido caía como se fosse de origem não permitida.
        if (!origensPermitidas.isBlank()) {
            http.cors(cors -> cors.configurationSource(corsConfigurationSource));
        }
        return http.build();
    }
}

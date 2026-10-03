package com.empresax.sistema.usuario;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cria o primeiro acesso irrestrito (dono/programador) a partir do .env, se ainda não existir um
 * usuário com aquele e-mail. Resolve o "ovo e galinha" de precisar de um usuário para criar usuários,
 * sem SQL manual. A senha nunca é logada.
 */
@Component
public class AcessoInicial implements ApplicationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(AcessoInicial.class);

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final String nome;
    private final String email;
    private final String senha;

    public AcessoInicial(
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder,
            @Value("${acesso-inicial.nome}") String nome,
            @Value("${acesso-inicial.email}") String email,
            @Value("${acesso-inicial.senha}") String senha
    ) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.nome = nome;
        this.email = email;
        this.senha = senha;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments argumentos) {
        if (email.isBlank() || senha.isBlank()) {
            return;
        }
        if (usuarioRepository.findByEmail(email.toLowerCase().trim()).isPresent()) {
            return;
        }
        usuarioRepository.save(Usuario.comAcessoIrrestrito(nome, email, passwordEncoder.encode(senha)));
        LOG.info("Acesso irrestrito inicial criado para {}", email);
    }
}

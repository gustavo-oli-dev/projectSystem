package com.empresax.sistema.usuario;

import com.empresax.sistema.acesso.Cargo;
import com.empresax.sistema.acesso.Permissao;
import com.empresax.sistema.common.domain.DomainException;
import com.empresax.sistema.usuario.whatsapp.TelefoneWhatsApp;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Usuário do painel. Ou tem acesso irrestrito (dono, programador), ou tem um cargo que define suas
 * permissões — nunca nenhum dos dois (o banco também garante isso com uma CHECK constraint).
 */
@Entity
@Table(name = "usuarios")
public class Usuario {

    private static final Pattern EMAIL_VALIDO = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String senhaCriptografada;

    @Column(nullable = false)
    private boolean acessoIrrestrito;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cargo_id")
    private Cargo cargo;

    @Column(nullable = false)
    private boolean ativo;

    /** Número verificado de onde esta pessoa conversa com o assistente pelo WhatsApp. */
    @Column(unique = true)
    private String telefoneWhatsapp;

    protected Usuario() {
        // exigido pelo JPA
    }

    private Usuario(String nome, String email, String senhaCriptografada, boolean acessoIrrestrito, Cargo cargo) {
        this.nome = validarNome(nome);
        this.email = validarEmail(email);
        this.senhaCriptografada = validarSenha(senhaCriptografada);
        this.acessoIrrestrito = acessoIrrestrito;
        this.cargo = cargo;
        this.ativo = true;
    }

    public static Usuario comCargo(String nome, String email, String senhaCriptografada, Cargo cargo) {
        if (cargo == null) {
            throw new DomainException("Perfil de acesso é obrigatório para quem não tem acesso irrestrito");
        }
        return new Usuario(nome, email, senhaCriptografada, false, cargo);
    }

    public static Usuario comAcessoIrrestrito(String nome, String email, String senhaCriptografada) {
        return new Usuario(nome, email, senhaCriptografada, true, null);
    }

    private static String validarNome(String nome) {
        if (nome == null || nome.isBlank()) {
            throw new DomainException("Nome do usuário é obrigatório");
        }
        return nome.trim();
    }

    private static String validarEmail(String email) {
        if (email == null || !EMAIL_VALIDO.matcher(email).matches()) {
            throw new DomainException("E-mail do usuário é inválido");
        }
        return email.toLowerCase().trim();
    }

    private static String validarSenha(String senhaCriptografada) {
        if (senhaCriptografada == null || senhaCriptografada.isBlank()) {
            throw new DomainException("Senha do usuário é obrigatória");
        }
        return senhaCriptografada;
    }

    public Set<Permissao> permissoes() {
        if (acessoIrrestrito) {
            return Collections.unmodifiableSet(EnumSet.allOf(Permissao.class));
        }
        return cargo.permissoes();
    }

    public boolean possui(Permissao permissao) {
        return acessoIrrestrito || cargo.possui(permissao);
    }

    /** Regra anti-escalada: só se concede a outra pessoa o que você mesmo tem. */
    public boolean podeConceder(Set<Permissao> permissoesConcedidas) {
        return acessoIrrestrito || permissoes().containsAll(permissoesConcedidas);
    }

    public void trocarCargo(Cargo novoCargo) {
        if (novoCargo == null) {
            throw new DomainException("Perfil de acesso é obrigatório");
        }
        this.cargo = novoCargo;
        this.acessoIrrestrito = false;
    }

    public void vincularWhatsApp(TelefoneWhatsApp telefone) {
        if (!possui(Permissao.ASSISTENTE_GESTOR_USAR)) {
            throw new DomainException("Seu perfil de acesso não inclui o assistente");
        }
        this.telefoneWhatsapp = telefone.numero();
    }

    public void desvincularWhatsApp() {
        this.telefoneWhatsapp = null;
    }

    /** O assistente só responde no WhatsApp a quem está ativo e ainda tem a permissão. */
    public boolean podeUsarAssistentePeloWhatsApp() {
        return ativo && possui(Permissao.ASSISTENTE_GESTOR_USAR);
    }

    public void desativar() {
        this.ativo = false;
    }

    public void ativar() {
        this.ativo = true;
    }

    public UUID id() {
        return id;
    }

    public String nome() {
        return nome;
    }

    public String email() {
        return email;
    }

    public String senhaCriptografada() {
        return senhaCriptografada;
    }

    public boolean acessoIrrestrito() {
        return acessoIrrestrito;
    }

    public Optional<Cargo> cargo() {
        return Optional.ofNullable(cargo);
    }

    public boolean ativo() {
        return ativo;
    }

    public Optional<String> telefoneWhatsapp() {
        return Optional.ofNullable(telefoneWhatsapp);
    }
}

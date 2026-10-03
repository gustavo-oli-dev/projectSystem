package com.empresax.sistema.usuario.whatsapp;

import com.empresax.sistema.common.domain.DomainException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Código enviado por WhatsApp para provar que o número é de quem está pedindo o vínculo. Sem essa
 * prova, alguém poderia cadastrar o número de outra pessoa e fazer o assistente mandar dados da
 * empresa para ela. Uma verificação pendente por usuário; o código é guardado só como hash.
 */
@Entity
@Table(name = "verificacoes_whatsapp")
public class VerificacaoWhatsApp {

    static final Duration VALIDADE = Duration.ofMinutes(10);
    static final Duration INTERVALO_MINIMO_ENTRE_ENVIOS = Duration.ofSeconds(60);
    static final int TENTATIVAS_PERMITIDAS = 5;

    @Id
    private UUID usuarioId;

    @Column(nullable = false)
    private String telefone;

    @Column(nullable = false)
    private String codigoHash;

    @Column(nullable = false)
    private Instant criadaEm;

    @Column(nullable = false)
    private int tentativasRestantes;

    protected VerificacaoWhatsApp() {
        // exigido pelo JPA
    }

    public VerificacaoWhatsApp(UUID usuarioId, TelefoneWhatsApp telefone, String codigo, Instant agora) {
        if (usuarioId == null) {
            throw new DomainException("Usuário da verificação é obrigatório");
        }
        if (codigo == null || codigo.isBlank()) {
            throw new DomainException("Código de verificação é obrigatório");
        }
        this.usuarioId = usuarioId;
        this.telefone = telefone.numero();
        this.codigoHash = hash(codigo);
        this.criadaEm = agora;
        this.tentativasRestantes = TENTATIVAS_PERMITIDAS;
    }

    /** Evita usar o sistema para disparar mensagens em série para um número. */
    public boolean permiteNovoEnvio(Instant agora) {
        return !agora.isBefore(criadaEm.plus(INTERVALO_MINIMO_ENTRE_ENVIOS));
    }

    /**
     * Confere o código digitado. Errar consome uma tentativa; esgotadas as tentativas ou vencido o
     * prazo, é preciso pedir um código novo.
     *
     * @return o telefone verificado, para vincular ao usuário
     */
    public TelefoneWhatsApp confirmar(String codigoInformado, Instant agora) {
        if (agora.isAfter(criadaEm.plus(VALIDADE))) {
            throw new DomainException("O código expirou. Peça um código novo.");
        }
        if (tentativasRestantes <= 0) {
            throw new DomainException("Tentativas esgotadas. Peça um código novo.");
        }
        boolean confere = codigoInformado != null && MessageDigest.isEqual(
                hash(codigoInformado.trim()).getBytes(StandardCharsets.UTF_8),
                codigoHash.getBytes(StandardCharsets.UTF_8));
        if (!confere) {
            tentativasRestantes--;
            throw new DomainException("Código incorreto. Tentativas restantes: " + tentativasRestantes);
        }
        return new TelefoneWhatsApp(telefone);
    }

    private static String hash(String codigo) {
        try {
            byte[] resumo = MessageDigest.getInstance("SHA-256").digest(codigo.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(resumo);
        } catch (NoSuchAlgorithmException falha) {
            throw new IllegalStateException("SHA-256 indisponível na JVM", falha);
        }
    }

    public UUID usuarioId() {
        return usuarioId;
    }

    public int tentativasRestantes() {
        return tentativasRestantes;
    }
}

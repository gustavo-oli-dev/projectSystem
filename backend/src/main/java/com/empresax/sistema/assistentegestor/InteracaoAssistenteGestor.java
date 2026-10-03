package com.empresax.sistema.assistentegestor;

import com.empresax.sistema.common.domain.DomainException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Registro de auditoria de cada pergunta/resposta do assistente do gestor (RNF04): toda resposta
 * automática da IA precisa ser rastreável.
 */
@Entity
@Table(name = "interacoes_assistente_gestor")
public class InteracaoAssistenteGestor {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, columnDefinition = "text")
    private String pergunta;

    @Column(nullable = false, columnDefinition = "text")
    private String resposta;

    @Column(columnDefinition = "text")
    private String ferramentasChamadas;

    @Column(nullable = false)
    private String solicitante;

    @Column(nullable = false, updatable = false)
    private Instant criadoEm;

    protected InteracaoAssistenteGestor() {
        // exigido pelo JPA
    }

    public InteracaoAssistenteGestor(String pergunta, String resposta, String ferramentasChamadas, String solicitante) {
        this.pergunta = validar(pergunta, "Pergunta");
        this.resposta = validar(resposta, "Resposta");
        this.ferramentasChamadas = ferramentasChamadas;
        this.solicitante = validar(solicitante, "Solicitante");
        this.criadoEm = Instant.now();
    }

    private static String validar(String valor, String nomeCampo) {
        if (valor == null || valor.isBlank()) {
            throw new DomainException(nomeCampo + " da interação com o assistente é obrigatória");
        }
        return valor;
    }

    public UUID id() {
        return id;
    }

    public String pergunta() {
        return pergunta;
    }

    public String resposta() {
        return resposta;
    }

    public String ferramentasChamadas() {
        return ferramentasChamadas;
    }

    public String solicitante() {
        return solicitante;
    }

    public Instant criadoEm() {
        return criadoEm;
    }
}

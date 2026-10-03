package com.empresax.sistema.produto.foto;

import com.empresax.sistema.common.domain.DomainException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Foto de produto guardada no PostgreSQL (D18): o backup diário cobre só o banco, então a foto
 * fica coberta junto. Tamanho limitado para o banco não inchar.
 */
@Entity
@Table(name = "fotos_produto")
public class FotoProduto {

    public static final int TAMANHO_MAXIMO_BYTES = 5 * 1024 * 1024;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, updatable = false)
    private UUID produtoId;

    @Column(nullable = false, updatable = false)
    private byte[] conteudo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private TipoImagem tipo;

    @Column(nullable = false, updatable = false)
    private int tamanhoBytes;

    @Column(nullable = false)
    private int ordem;

    @Column(nullable = false, updatable = false)
    private Instant criadaEm;

    protected FotoProduto() {
        // exigido pelo JPA
    }

    public FotoProduto(UUID produtoId, byte[] conteudo, int ordem) {
        if (produtoId == null) {
            throw new DomainException("Foto precisa estar vinculada a um produto");
        }
        if (conteudo == null || conteudo.length == 0) {
            throw new DomainException("A foto está vazia");
        }
        if (conteudo.length > TAMANHO_MAXIMO_BYTES) {
            throw new DomainException("A foto deve ter no máximo 5 MB");
        }
        this.tipo = TipoImagem.detectar(conteudo)
                .orElseThrow(() -> new DomainException("Formato de foto não aceito. Use JPG, PNG ou WEBP."));
        this.produtoId = produtoId;
        this.conteudo = conteudo.clone();
        this.tamanhoBytes = conteudo.length;
        this.ordem = ordem;
        this.criadaEm = Instant.now();
    }

    public UUID id() {
        return id;
    }

    public UUID produtoId() {
        return produtoId;
    }

    public byte[] conteudo() {
        return conteudo.clone();
    }

    public TipoImagem tipo() {
        return tipo;
    }
}

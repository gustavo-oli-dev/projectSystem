package com.empresax.sistema.atendimento.mensagem;

import com.empresax.sistema.common.domain.DomainException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AnexoTest {

    @Test
    void criaAnexoComOsDadosInformados() {
        UUID mensagemId = UUID.randomUUID();

        Anexo anexo = new Anexo(mensagemId, "image/png", 1024L, "abc123", "chave-objeto-1", "foto.png");

        assertThat(anexo.mensagemId()).isEqualTo(mensagemId);
        assertThat(anexo.tamanhoBytes()).isEqualTo(1024L);
    }

    @Test
    void rejeitaTamanhoZeroOuNegativo() {
        assertThatThrownBy(() -> new Anexo(UUID.randomUUID(), "image/png", 0L, "abc123", "chave-1", "foto.png"))
                .isInstanceOf(DomainException.class);
    }

    @Test
    void rejeitaMimeTypeEmBranco() {
        assertThatThrownBy(() -> new Anexo(UUID.randomUUID(), " ", 1024L, "abc123", "chave-1", "foto.png"))
                .isInstanceOf(DomainException.class);
    }
}

package com.empresax.sistema.shared.armazenamento;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Regressão de um defeito achado no primeiro deploy no Render (D48): o SDK do MinIO recusa
 * access-key/secret-key vazios já no construtor (IllegalArgumentException), antes de qualquer
 * tentativa de conexão — e isso derrubava o boot da aplicação inteira quando as variáveis de
 * ambiente do MinIO não estavam configuradas (não há MinIO no deploy). O construtor precisa
 * aceitar credenciais vazias sem lançar; a conexão de verdade (ou a falta dela) só aparece depois,
 * de forma não-fatal, no @PostConstruct.
 */
class MinioArmazenamentoObjetosTest {

    @Test
    void naoLancaAoReceberCredenciaisVazias() {
        assertThatCode(() -> new MinioArmazenamentoObjetos("http://minio:9000", "", "", "anexos-whatsapp"))
                .doesNotThrowAnyException();
    }

    @Test
    void naoLancaAoReceberCredenciaisNulas() {
        assertThatCode(() -> new MinioArmazenamentoObjetos("http://minio:9000", null, null, "anexos-whatsapp"))
                .doesNotThrowAnyException();
    }

    @Test
    void continuaFuncionandoComCredenciaisDeVerdade() {
        assertThatCode(() -> new MinioArmazenamentoObjetos("http://minio:9000", "usuario", "senha-secreta", "anexos-whatsapp"))
                .doesNotThrowAnyException();
    }
}

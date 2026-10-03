package com.empresax.sistema.cobranca.pagamento;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AssinaturaMercadoPagoValidadorTest {

    private final AssinaturaMercadoPagoValidador validador = new AssinaturaMercadoPagoValidador("segredo-de-teste");

    @Test
    void aceitaAssinaturaCalculadaComOMesmoSegredo() {
        String dataId = "999";
        String idRequisicao = "req-1";
        String timestamp = "1700000000";
        String manifesto = "id:" + dataId + ";request-id:" + idRequisicao + ";ts:" + timestamp + ";";
        String assinaturaValida = validador.calcularHmac(manifesto);
        String cabecalho = "ts=" + timestamp + ",v1=" + assinaturaValida;

        assertThat(validador.valida(cabecalho, idRequisicao, dataId)).isTrue();
    }

    @Test
    void rejeitaAssinaturaAdulterada() {
        String cabecalho = "ts=1700000000,v1=assinatura-forjada";

        assertThat(validador.valida(cabecalho, "req-1", "999")).isFalse();
    }

    @Test
    void rejeitaQuandoFaltaCabecalho() {
        assertThat(validador.valida(null, "req-1", "999")).isFalse();
    }
}

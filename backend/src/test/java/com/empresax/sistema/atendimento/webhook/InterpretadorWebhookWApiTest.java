package com.empresax.sistema.atendimento.webhook;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class InterpretadorWebhookWApiTest {

    private final InterpretadorWebhookWApi interpretador = new InterpretadorWebhookWApi();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void extraiTelefoneTextoEIdDeMensagemRecebida() throws Exception {
        Optional<MensagemRecebida> resultado = interpretador.interpretar(json("""
                {"messageId":"ABC1","fromMe":false,"isGroup":false,
                 "sender":{"id":"5585988887777@s.whatsapp.net"},
                 "msgContent":{"conversation":"Oi, e meu pedido?"}}
                """));

        assertThat(resultado).contains(new MensagemRecebida("5585988887777", "Oi, e meu pedido?", "ABC1"));
    }

    @Test
    void aceitaTextoNoFormatoEstendido() throws Exception {
        Optional<MensagemRecebida> resultado = interpretador.interpretar(json("""
                {"fromMe":false,"chat":{"id":"5585988887777"},
                 "msgContent":{"extendedTextMessage":{"text":"Com link"}}}
                """));

        assertThat(resultado).map(MensagemRecebida::texto).contains("Com link");
    }

    @Test
    void ignoraMensagemEnviadaPelaPropriaEmpresa() throws Exception {
        assertThat(interpretador.interpretar(json("""
                {"fromMe":true,"sender":{"id":"5585988887777"},"msgContent":{"conversation":"x"}}
                """))).isEmpty();
    }

    @Test
    void ignoraMensagemDeGrupo() throws Exception {
        assertThat(interpretador.interpretar(json("""
                {"isGroup":true,"sender":{"id":"5585988887777"},"msgContent":{"conversation":"x"}}
                """))).isEmpty();
    }

    private JsonNode json(String texto) throws Exception {
        return objectMapper.readTree(texto);
    }
}

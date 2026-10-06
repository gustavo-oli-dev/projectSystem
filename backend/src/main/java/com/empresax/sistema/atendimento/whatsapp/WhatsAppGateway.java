package com.empresax.sistema.atendimento.whatsapp;

/**
 * Porta para o envio de mensagens de WhatsApp. Isolar a interface permite trocar a API não
 * oficial (decisão D2 em DECISOES.md) pela API oficial da Meta no futuro sem tocar em
 * MensagemService.
 */
public interface WhatsAppGateway {

    /**
     * @return o id externo da mensagem enviada, para correlacionar com confirmações futuras
     */
    String enviarTexto(String telefoneDestino, String texto);

    /**
     * Envia uma imagem com legenda (ex.: foto do produto que o cliente pediu) — D43.
     *
     * @return o id externo da mensagem enviada
     */
    String enviarImagem(String telefoneDestino, byte[] imagem, String tipoMime, String legenda);
}

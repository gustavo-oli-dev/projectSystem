package com.empresax.sistema.ia;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Loop de tool use da Claude: manda as mensagens, executa as ferramentas que o modelo pedir, devolve
 * os resultados e repete até vir uma resposta em texto. Compartilhado pelo assistente do gestor e
 * pelo bot de atendimento — cada um fornece o próprio cliente, prompt, ferramentas e executor.
 */
@Component
public class ConversaComFerramentas {

    private static final int LIMITE_RODADAS_DE_FERRAMENTA = 5;
    private static final String MOTIVO_PARADA_FERRAMENTA = "tool_use";

    private final ObjectMapper objectMapper = new ObjectMapper();

    public record Resultado(String resposta, List<String> ferramentasChamadas) {
    }

    @FunctionalInterface
    public interface ExecutorFerramenta {
        JsonNode executar(String nome, JsonNode entrada);
    }

    public Resultado conduzir(
            ClaudeApiCliente cliente,
            String promptSistema,
            String definicaoFerramentasJson,
            ArrayNode mensagens,
            ExecutorFerramenta executor
    ) {
        ArrayNode ferramentas = lerFerramentas(definicaoFerramentasJson);
        List<String> ferramentasChamadas = new ArrayList<>();

        for (int rodada = 0; rodada < LIMITE_RODADAS_DE_FERRAMENTA; rodada++) {
            JsonNode resposta = cliente.enviarMensagens(promptSistema, mensagens, ferramentas);

            if (!MOTIVO_PARADA_FERRAMENTA.equals(resposta.path("stop_reason").asText(""))) {
                return new Resultado(extrairTexto(resposta), List.copyOf(ferramentasChamadas));
            }

            processarRodada(resposta, mensagens, ferramentasChamadas, executor);
        }
        throw new IntegracaoIaException("A IA excedeu o limite de rodadas de ferramenta sem concluir");
    }

    public ObjectNode mensagemDeTexto(String papel, String texto) {
        ObjectNode mensagem = objectMapper.createObjectNode();
        mensagem.put("role", papel);
        mensagem.put("content", texto);
        return mensagem;
    }

    public ArrayNode novaListaDeMensagens() {
        return objectMapper.createArrayNode();
    }

    public JsonNode paraJson(Object valor) {
        return objectMapper.valueToTree(valor);
    }

    private void processarRodada(
            JsonNode resposta,
            ArrayNode mensagens,
            List<String> ferramentasChamadas,
            ExecutorFerramenta executor
    ) {
        ObjectNode mensagemAssistente = objectMapper.createObjectNode();
        mensagemAssistente.put("role", "assistant");
        mensagemAssistente.set("content", resposta.path("content").deepCopy());
        mensagens.add(mensagemAssistente);

        ArrayNode resultados = objectMapper.createArrayNode();
        for (JsonNode bloco : resposta.path("content")) {
            if (!"tool_use".equals(bloco.path("type").asText())) {
                continue;
            }
            String nome = bloco.path("name").asText();
            ferramentasChamadas.add(nome);

            ObjectNode resultado = objectMapper.createObjectNode();
            resultado.put("type", "tool_result");
            resultado.put("tool_use_id", bloco.path("id").asText());
            resultado.put("content", executor.executar(nome, bloco.path("input")).toString());
            resultados.add(resultado);
        }

        ObjectNode mensagemResultados = objectMapper.createObjectNode();
        mensagemResultados.put("role", "user");
        mensagemResultados.set("content", resultados);
        mensagens.add(mensagemResultados);
    }

    private ArrayNode lerFerramentas(String definicaoJson) {
        try {
            return (ArrayNode) objectMapper.readTree(definicaoJson);
        } catch (JsonProcessingException excecao) {
            throw new IntegracaoIaException("Definição de ferramentas inválida", excecao);
        }
    }

    private static String extrairTexto(JsonNode resposta) {
        StringBuilder texto = new StringBuilder();
        for (JsonNode bloco : resposta.path("content")) {
            if ("text".equals(bloco.path("type").asText())) {
                texto.append(bloco.path("text").asText());
            }
        }
        return texto.toString().trim();
    }
}

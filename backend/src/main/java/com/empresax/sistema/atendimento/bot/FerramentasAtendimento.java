package com.empresax.sistema.atendimento.bot;

import com.empresax.sistema.atendimento.conversa.Conversa;
import com.empresax.sistema.ia.ConversaComFerramentas;
import com.empresax.sistema.pedido.PedidoRepository;
import com.empresax.sistema.produto.ProdutoRepository;
import com.empresax.sistema.servico.ServicoRepository;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Ferramentas do bot de atendimento ao cliente. Regras de segurança (decisões da análise inicial):
 * - a identidade do cliente vem da Conversa (resolvida pelo servidor a partir do telefone), nunca de
 *   um parâmetro que o modelo possa preencher — mesmo enganado, o modelo não acessa outro cliente;
 * - só consulta, nunca muda estado de pedido/cobrança/nota;
 * - minimização de dados: nada de CPF/CNPJ ou dados de pagamento nas respostas.
 */
@Component
public class FerramentasAtendimento {

    static final String DEFINICAO = """
            [
              {
                "name": "consultar_meus_pedidos",
                "description": "Lista os pedidos mais recentes do cliente desta conversa (código, situação, total e data)",
                "input_schema": { "type": "object", "properties": {} }
              },
              {
                "name": "consultar_catalogo",
                "description": "Lista os produtos e serviços disponíveis com seus preços",
                "input_schema": { "type": "object", "properties": {} }
              },
              {
                "name": "transferir_para_atendente",
                "description": "Transfere a conversa para um atendente humano. Use quando o cliente pedir uma pessoa, estiver insatisfeito, ou o assunto fugir do que as outras ferramentas cobrem",
                "input_schema": {
                  "type": "object",
                  "properties": { "motivo": { "type": "string", "description": "Motivo curto da transferência" } },
                  "required": ["motivo"]
                }
              }
            ]
            """;

    private static final Map<String, String> SITUACAO_PEDIDO = Map.of(
            "ABERTO", "em aberto",
            "AGUARDANDO_EMISSAO", "confirmado, aguardando emissão da nota",
            "CONCLUIDO", "concluído",
            "CANCELADO", "cancelado"
    );

    private final PedidoRepository pedidoRepository;
    private final ProdutoRepository produtoRepository;
    private final ServicoRepository servicoRepository;
    private final ConversaComFerramentas conversaComFerramentas;

    public FerramentasAtendimento(
            PedidoRepository pedidoRepository,
            ProdutoRepository produtoRepository,
            ServicoRepository servicoRepository,
            ConversaComFerramentas conversaComFerramentas
    ) {
        this.pedidoRepository = pedidoRepository;
        this.produtoRepository = produtoRepository;
        this.servicoRepository = servicoRepository;
        this.conversaComFerramentas = conversaComFerramentas;
    }

    ConversaComFerramentas.ExecutorFerramenta executorPara(Conversa conversa, PedidoDeTransferencia transferencia) {
        return (nome, entrada) -> switch (nome) {
            case "consultar_meus_pedidos" -> consultarPedidos(conversa);
            case "consultar_catalogo" -> consultarCatalogo();
            case "transferir_para_atendente" -> transferir(entrada, transferencia);
            default -> conversaComFerramentas.paraJson(Map.of("erro", "Ferramenta desconhecida: " + nome));
        };
    }

    private JsonNode consultarPedidos(Conversa conversa) {
        if (conversa.clienteId() == null) {
            return conversaComFerramentas.paraJson(Map.of(
                    "erro", "Cliente não identificado por este número. Ofereça transferir para um atendente."));
        }
        List<Map<String, String>> pedidos = pedidoRepository.findTop10ByClienteIdOrderByCriadoEmDesc(conversa.clienteId())
                .stream()
                .map(pedido -> Map.of(
                        "codigo", pedido.id().toString().substring(0, 8),
                        "situacao", SITUACAO_PEDIDO.getOrDefault(pedido.status().name(), pedido.status().name()),
                        "total", pedido.valorTotal().valor().toPlainString(),
                        "data", pedido.criadoEm().toString().substring(0, 10)))
                .toList();
        return conversaComFerramentas.paraJson(Map.of("pedidos", pedidos));
    }

    private JsonNode consultarCatalogo() {
        List<Map<String, String>> produtos = produtoRepository.findByAtivoTrue().stream()
                .map(produto -> Map.of(
                        "nome", produto.nome(),
                        "preco", produto.precoUnitario().valor().toPlainString(),
                        "disponivel", produto.quantidadeEmEstoque() > 0 ? "sim" : "esgotado"))
                .toList();
        List<Map<String, String>> servicos = servicoRepository.findByAtivoTrue().stream()
                .map(servico -> Map.of("nome", servico.nome(), "preco", servico.precoUnitario().valor().toPlainString()))
                .toList();
        return conversaComFerramentas.paraJson(Map.of("produtos", produtos, "servicos", servicos));
    }

    private JsonNode transferir(JsonNode entrada, PedidoDeTransferencia transferencia) {
        transferencia.registrar(entrada.path("motivo").asText(null));
        return conversaComFerramentas.paraJson(Map.of("resultado", "Conversa será transferida a um atendente."));
    }
}

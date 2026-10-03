package com.empresax.sistema.assistentegestor;

import com.empresax.sistema.acesso.Permissao;
import com.empresax.sistema.ia.ClaudeApiCliente;
import com.empresax.sistema.ia.ConfiguracaoClaude;
import com.empresax.sistema.ia.ConversaComFerramentas;
import com.empresax.sistema.relatorios.ConsultasGerenciais;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Assistente de IA do gestor (RF02, D6): somente leitura. Respeita o cargo de quem pergunta — cada
 * ferramenta exige uma permissão; a IA só recebe as ferramentas que o cargo permite, e a execução
 * confere de novo. Sem isso o assistente viraria uma porta dos fundos para dados que a pessoa não
 * pode ver na tela (ex.: faturamento). Toda interação é auditada com o solicitante (RNF04).
 */
@Service
public class AssistenteGestorService {

    private static final String PROMPT_SISTEMA = """
            Você é o assistente interno da Empresa X para a equipe de gestão. Responda SOMENTE com \
            base no resultado das ferramentas disponíveis — nunca invente números. Se a pergunta \
            exigir uma informação que nenhuma ferramenta disponível fornece, diga que não tem acesso \
            a esse dado. Você só consulta: nunca execute nem diga ter executado uma ação. Responda em \
            português, de forma direta e curta.""";

    private static final String RESPOSTA_SEM_FERRAMENTAS =
            "Seu perfil de acesso não dá acesso a nenhuma informação que eu consiga consultar.";

    private static final Map<String, Ferramenta> FERRAMENTAS = new LinkedHashMap<>();

    static {
        FERRAMENTAS.put("consultar_faturamento", new Ferramenta(Permissao.FATURAMENTO_VER, """
                {"name":"consultar_faturamento",
                 "description":"Consulta o faturamento (soma de cobranças já pagas) num período de datas",
                 "input_schema":{"type":"object","properties":{
                   "data_inicio":{"type":"string","description":"Data inicial AAAA-MM-DD"},
                   "data_fim":{"type":"string","description":"Data final AAAA-MM-DD"}},
                   "required":["data_inicio","data_fim"]}}"""));
        FERRAMENTAS.put("consultar_pedidos_por_status", new Ferramenta(Permissao.PEDIDOS_VER, """
                {"name":"consultar_pedidos_por_status",
                 "description":"Conta quantos pedidos existem em cada status (aberto, aguardando emissão, concluído, cancelado)",
                 "input_schema":{"type":"object","properties":{}}}"""));
        FERRAMENTAS.put("consultar_cobrancas_pendentes", new Ferramenta(Permissao.COBRANCAS_VER, """
                {"name":"consultar_cobrancas_pendentes",
                 "description":"Lista as cobranças ainda não pagas",
                 "input_schema":{"type":"object","properties":{}}}"""));
    }

    private record Ferramenta(Permissao permissaoExigida, String definicaoJson) {
    }

    private final ClaudeApiCliente claudeGestor;
    private final ConversaComFerramentas conversaComFerramentas;
    private final ConsultasGerenciais consultas;
    private final InteracaoAssistenteGestorRepository interacaoRepository;

    public AssistenteGestorService(
            @Qualifier(ConfiguracaoClaude.CLAUDE_GESTOR) ClaudeApiCliente claudeGestor,
            ConversaComFerramentas conversaComFerramentas,
            ConsultasGerenciais consultas,
            InteracaoAssistenteGestorRepository interacaoRepository
    ) {
        this.claudeGestor = claudeGestor;
        this.conversaComFerramentas = conversaComFerramentas;
        this.consultas = consultas;
        this.interacaoRepository = interacaoRepository;
    }

    @Transactional
    public String responder(String pergunta, Set<Permissao> permissoesDoSolicitante, String solicitante) {
        List<String> definicoesPermitidas = FERRAMENTAS.values().stream()
                .filter(ferramenta -> permissoesDoSolicitante.contains(ferramenta.permissaoExigida()))
                .map(Ferramenta::definicaoJson)
                .toList();

        ConversaComFerramentas.Resultado resultado = definicoesPermitidas.isEmpty()
                ? new ConversaComFerramentas.Resultado(RESPOSTA_SEM_FERRAMENTAS, List.of())
                : consultarIa(pergunta, definicoesPermitidas, permissoesDoSolicitante);

        interacaoRepository.save(new InteracaoAssistenteGestor(
                pergunta, resultado.resposta(), String.join(", ", resultado.ferramentasChamadas()), solicitante));
        return resultado.resposta();
    }

    private ConversaComFerramentas.Resultado consultarIa(String pergunta, List<String> definicoes, Set<Permissao> permissoes) {
        ArrayNode mensagens = conversaComFerramentas.novaListaDeMensagens();
        mensagens.add(conversaComFerramentas.mensagemDeTexto("user", pergunta));
        return conversaComFerramentas.conduzir(
                claudeGestor,
                PROMPT_SISTEMA,
                "[" + String.join(",", definicoes) + "]",
                mensagens,
                (nome, entrada) -> executar(nome, entrada, permissoes));
    }

    private JsonNode executar(String nome, JsonNode entrada, Set<Permissao> permissoes) {
        Ferramenta ferramenta = FERRAMENTAS.get(nome);
        if (ferramenta == null || !permissoes.contains(ferramenta.permissaoExigida())) {
            return conversaComFerramentas.paraJson(Map.of("erro", "Ferramenta indisponível para este usuário"));
        }
        return switch (nome) {
            case "consultar_faturamento" -> conversaComFerramentas.paraJson(consultas.consultarFaturamento(
                    LocalDate.parse(entrada.path("data_inicio").asText()),
                    LocalDate.parse(entrada.path("data_fim").asText())));
            case "consultar_pedidos_por_status" -> conversaComFerramentas.paraJson(consultas.consultarPedidosPorStatus());
            case "consultar_cobrancas_pendentes" -> conversaComFerramentas.paraJson(consultas.consultarCobrancasPendentes());
            default -> conversaComFerramentas.paraJson(Map.of("erro", "Ferramenta desconhecida: " + nome));
        };
    }
}

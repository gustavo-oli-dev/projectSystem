import { consultarCobrancasPendentes, consultarPedidosPorStatus } from "../../api/painelApi.js";
import { listarProdutos } from "../../api/produtosApi.js";
import {
  baixarCsvCaixa,
  baixarCsvVendas,
  gerarRelatorioCaixa,
  gerarRelatorioPerdas,
  listarItensCancelados,
  gerarRelatorioVendas,
  type Periodo,
  type RelatorioVendas,
} from "../../api/relatoriosApi.js";
import { possui } from "../../state/sessaoState.js";
import { elementoCarregando } from "../estadoCarregamento.js";
import { cartaoEstado } from "../estadoCard.js";
import { formatarMoeda } from "../formatarMoeda.js";
import { formatarInteiro } from "../formatarNumero.js";
import { criarGraficoRosca, type CorFatia, type Fatia } from "../graficos/graficoRosca.js";
import { situacaoEstoque } from "../produtos/situacaoEstoque.js";
import { criarIndicadores } from "./indicadoresView.js";
import { ATALHOS, periodoDoAtalho, type AtalhoPeriodo } from "./periodoPainel.js";
import { baixarLancamentos, gerarFluxoDeCaixa } from "../../api/financeiroApi.js";
import { montarAbaDinheiroDoDia } from "./abaDinheiroDoDia.js";
import { montarAbaFinanceiro } from "./abaFinanceiro.js";
import { montarAbaCaixa } from "./secoesCaixa.js";
import { montarSecaoPerdas } from "./secoesPerdas.js";
import {
  criarSecaoCanais,
  criarSecaoCustoLucro,
  criarSecaoDiasDaSemana,
  criarSecaoFaturamento,
  criarSecaoFormasPagamento,
  criarSecaoHorarios,
  criarSecaoMaisVendidos,
} from "./secoesRelatorio.js";

type AbaPainel = "vendas" | "produtos" | "horarios" | "caixa" | "dinheiro-do-dia" | "financeiro" | "operacao";

interface DefinicaoAba {
  aba: AbaPainel;
  rotulo: string;
  visivel: () => boolean;
  /** Operação é "agora" e o Dinheiro do dia tem o seu próprio dia: não usam o filtro de período. */
  usaPeriodo: boolean;
}

const ABAS: readonly DefinicaoAba[] = [
  { aba: "vendas", rotulo: "Vendas", visivel: () => possui("PAINEL_VENDAS"), usaPeriodo: true },
  { aba: "produtos", rotulo: "Produtos", visivel: () => possui("PAINEL_PRODUTOS"), usaPeriodo: true },
  { aba: "horarios", rotulo: "Horários", visivel: () => possui("PAINEL_HORARIOS"), usaPeriodo: true },
  { aba: "caixa", rotulo: "Caixa", visivel: () => possui("PAINEL_CAIXA"), usaPeriodo: true },
  {
    aba: "dinheiro-do-dia", rotulo: "Dinheiro do dia",
    visivel: () => possui("PAINEL_DINHEIRO_DO_DIA"), usaPeriodo: false,
  },
  { aba: "financeiro", rotulo: "Financeiro", visivel: () => possui("FINANCEIRO_VER"), usaPeriodo: true },
  {
    aba: "operacao", rotulo: "Operação agora",
    visivel: () => possui("PAINEL_OPERACAO"), usaPeriodo: false,
  },
];
const ATALHO_INICIAL: AtalhoPeriodo = "TRINTA_DIAS";
const ESTOQUE_BAIXO_MAXIMO = 8;
const STATUS_PEDIDO: Record<string, { rotulo: string; cor: CorFatia }> = {
  ABERTO: { rotulo: "Abertos", cor: 1 },
  AGUARDANDO_EMISSAO: { rotulo: "Aguardando nota", cor: 2 },
  CONCLUIDO: { rotulo: "Concluídos", cor: 3 },
  CANCELADO: { rotulo: "Cancelados", cor: "restante" },
};
const COR_STATUS_DESCONHECIDO: CorFatia = 5;

/**
 * Painel = central de relatórios, em abas (D28): Vendas, Produtos, Horários, Caixa, Dinheiro do dia,
 * Financeiro e Operação.
 * Um filtro de período só, no topo, vale para todas as abas que dependem de período. A aba aberta
 * fica no endereço (#/painel/caixa) para dar para voltar direto nela.
 */
export function montarPainel(container: HTMLElement, abaPedida: string | null): void {
  const titulo = document.createElement("h1");
  titulo.textContent = "Painel";
  const cabecalho = document.createElement("div");
  cabecalho.className = "cabecalho-pagina";
  cabecalho.append(titulo);

  const abas = ABAS.filter((definicao) => definicao.visivel());
  const primeira = abas[0];
  if (primeira === undefined) {
    container.replaceChildren(cabecalho, cartaoEstado("Seu perfil de acesso não inclui nenhum relatório do painel."));
    return;
  }

  let abaAtiva = abas.find((definicao) => definicao.aba === abaPedida) ?? primeira;
  let periodo = periodoDoAtalho(ATALHO_INICIAL);
  const relatoriosDeVendas = new Map<string, Promise<RelatorioVendas>>();
  const vendasDoPeriodo = (): Promise<RelatorioVendas> => {
    const chave = `${periodo.inicio}|${periodo.fim}`;
    const existente = relatoriosDeVendas.get(chave);
    if (existente !== undefined) {
      return existente;
    }
    const novo = gerarRelatorioVendas(periodo);
    relatoriosDeVendas.set(chave, novo);
    novo.catch(() => relatoriosDeVendas.delete(chave));
    return novo;
  };

  const conteudo = document.createElement("div");
  conteudo.className = "painel-relatorio";
  const filtro = criarFiltroPeriodo((novo) => {
    periodo = novo;
    void mostrarAba();
  });
  const botoes = new Map<AbaPainel, HTMLButtonElement>();
  const barraAbas = document.createElement("div");
  barraAbas.className = "abas";
  barraAbas.setAttribute("role", "tablist");
  for (const definicao of abas) {
    const botao = document.createElement("button");
    botao.type = "button";
    botao.className = "abas__item";
    botao.setAttribute("role", "tab");
    botao.textContent = definicao.rotulo;
    botao.addEventListener("click", () => {
      abaAtiva = definicao;
      // Troca de aba sem recarregar a tela (o período escolhido continua valendo).
      history.replaceState(null, "", `#/painel/${definicao.aba}`);
      void mostrarAba();
    });
    botoes.set(definicao.aba, botao);
    barraAbas.append(botao);
  }

  const mostrarAba = async (): Promise<void> => {
    botoes.forEach((botao, aba) => {
      const ativa = aba === abaAtiva.aba;
      botao.classList.toggle("abas__item--ativa", ativa);
      botao.setAttribute("aria-selected", String(ativa));
    });
    filtro.hidden = !abaAtiva.usaPeriodo;
    const abaPedidaAgora = abaAtiva.aba;
    // Recarregar mantém o conteúdo anterior esmaecido (sem piscar a tela) até chegar o novo.
    if (conteudo.childElementCount === 0) {
      conteudo.append(elementoCarregando("Calculando o relatório..."));
    }
    conteudo.classList.add("painel-relatorio--atualizando");
    try {
      const elementos = await montarConteudoDaAba(abaPedidaAgora, periodo, vendasDoPeriodo);
      if (abaAtiva.aba === abaPedidaAgora) {
        conteudo.replaceChildren(...elementos);
      }
    } catch {
      conteudo.replaceChildren(cartaoEstado("Não foi possível calcular o relatório deste período.", "erro"));
    } finally {
      conteudo.classList.remove("painel-relatorio--atualizando");
    }
  };

  container.replaceChildren(cabecalho, barraAbas, filtro, conteudo);
  void mostrarAba();
}

async function montarConteudoDaAba(
  aba: AbaPainel, periodo: Periodo, vendasDoPeriodo: () => Promise<RelatorioVendas>
): Promise<HTMLElement[]> {
  switch (aba) {
    case "vendas":
      return montarAbaVendas(await vendasDoPeriodo(), periodo);
    case "produtos": {
      const [vendas, perdas] = await Promise.all([vendasDoPeriodo(), gerarRelatorioPerdas(periodo)]);
      return [criarSecaoMaisVendidos(vendas, criarBotaoExportar(
        () => baixarCsvVendas(periodo, "mais-vendidos"), `mais-vendidos_${periodo.inicio}_a_${periodo.fim}.csv`)),
      montarSecaoPerdas(perdas)];
    }
    case "horarios":
      return montarAbaHorarios(await vendasDoPeriodo());
    case "caixa":
      return montarAbaCaixa(await gerarRelatorioCaixa(periodo), await listarItensCancelados(periodo), criarBotaoExportar(
        () => baixarCsvCaixa(periodo), `fechamentos-de-caixa_${periodo.inicio}_a_${periodo.fim}.csv`));
    case "dinheiro-do-dia":
      return montarAbaDinheiroDoDia();
    case "financeiro":
      return montarAbaFinanceiro(await gerarFluxoDeCaixa(periodo), criarBotaoExportar(
        () => baixarLancamentos(periodo), `financeiro_${periodo.inicio}_a_${periodo.fim}.csv`));
    case "operacao":
      return montarAbaOperacao();
  }
}

/** Grade de 3 colunas: faturamento no tempo inteiro em cima, as três pizzas embaixo. */
function montarAbaVendas(relatorio: RelatorioVendas, periodo: Periodo): HTMLElement[] {
  const faturamento = criarSecaoFaturamento(relatorio);
  faturamento.querySelector(".cartao-relatorio__cabecalho")?.append(criarBotaoExportar(
    () => baixarCsvVendas(periodo, "periodos"), `periodos_${periodo.inicio}_a_${periodo.fim}.csv`));
  return [
    criarIndicadores(relatorio),
    faturamento,
    linha(criarSecaoFormasPagamento(relatorio), criarSecaoCanais(relatorio), criarSecaoCustoLucro(relatorio)),
  ];
}

function montarAbaHorarios(relatorio: RelatorioVendas): HTMLElement[] {
  const horarios = criarSecaoHorarios(relatorio);
  horarios.classList.add("cartao-relatorio--largo");
  return [linha(horarios, criarSecaoDiasDaSemana(relatorio))];
}

/** Filtro único acima do conteúdo: atalhos de período + datas livres. */
function criarFiltroPeriodo(aoMudar: (periodo: Periodo) => void): HTMLElement {
  const botoes = ATALHOS.map(({ atalho, rotulo }) => {
    const botao = document.createElement("button");
    botao.type = "button";
    botao.className = "filtro-periodo__atalho";
    botao.textContent = rotulo;
    botao.setAttribute("aria-pressed", String(atalho === ATALHO_INICIAL));
    botao.addEventListener("click", () => {
      marcar(botao);
      const periodo = periodoDoAtalho(atalho);
      inicio.value = periodo.inicio;
      fim.value = periodo.fim;
      aoMudar(periodo);
    });
    return botao;
  });

  const inicial = periodoDoAtalho(ATALHO_INICIAL);
  const inicio = criarData("Início do período", inicial.inicio);
  const fim = criarData("Fim do período", inicial.fim);
  const aplicar = document.createElement("button");
  aplicar.type = "button";
  aplicar.className = "btn btn-ghost btn-pequeno";
  aplicar.textContent = "Aplicar datas";
  aplicar.addEventListener("click", () => {
    if (inicio.value === "" || fim.value === "" || fim.value < inicio.value) {
      return;
    }
    marcar(null);
    aoMudar({ inicio: inicio.value, fim: fim.value });
  });

  const marcar = (ativo: HTMLButtonElement | null): void => {
    botoes.forEach((botao) => botao.setAttribute("aria-pressed", String(botao === ativo)));
  };

  const grupoAtalhos = document.createElement("div");
  grupoAtalhos.className = "filtro-periodo__atalhos";
  grupoAtalhos.setAttribute("role", "group");
  grupoAtalhos.setAttribute("aria-label", "Período");
  grupoAtalhos.append(...botoes);

  const separador = document.createElement("span");
  separador.className = "filtro-periodo__ate";
  separador.textContent = "até";
  const datas = document.createElement("div");
  datas.className = "filtro-periodo__datas";
  datas.append(inicio, separador, fim, aplicar);

  const filtro = document.createElement("div");
  filtro.className = "filtro-periodo";
  filtro.append(grupoAtalhos, datas);
  return filtro;
}

function criarData(rotulo: string, valor: string): HTMLInputElement {
  const campo = document.createElement("input");
  campo.type = "date";
  campo.value = valor;
  campo.setAttribute("aria-label", rotulo);
  return campo;
}

function criarBotaoExportar(baixar: () => Promise<Blob>, nomeArquivo: string): HTMLButtonElement {
  const botao = document.createElement("button");
  botao.type = "button";
  botao.className = "btn btn-ghost btn-pequeno";
  botao.textContent = "Exportar CSV";
  botao.addEventListener("click", () => {
    botao.disabled = true;
    baixar()
      .then((arquivo) => salvarArquivo(arquivo, nomeArquivo))
      .catch(() => {
        botao.textContent = "Falhou — tentar de novo";
      })
      .finally(() => {
        botao.disabled = false;
      });
  });
  return botao;
}

function salvarArquivo(arquivo: Blob, nome: string): void {
  const endereco = URL.createObjectURL(arquivo);
  const link = document.createElement("a");
  link.href = endereco;
  link.download = nome;
  link.click();
  URL.revokeObjectURL(endereco);
}

function linha(...cartoes: HTMLElement[]): HTMLElement {
  const grade = document.createElement("div");
  grade.className = "linha-relatorio";
  grade.append(...cartoes);
  return grade;
}

/** Parte operacional (sem valores de faturamento): cada bloco só aparece com a permissão dele. */
function montarAbaOperacao(): HTMLElement[] {
  const blocos: HTMLElement[] = [];
  if (possui("CATALOGO_VER")) {
    blocos.push(blocoAssincrono("Estoque baixo", carregarEstoqueBaixo));
  }
  if (possui("PEDIDOS_VER")) {
    blocos.push(blocoAssincrono("Pedidos por situação", carregarPedidosPorStatus));
  }
  if (possui("COBRANCAS_VER")) {
    blocos.push(blocoAssincrono("Recebimentos pendentes", carregarCobrancasPendentes));
  }
  return [linha(...blocos)];
}

function blocoAssincrono(titulo: string, carregar: () => Promise<HTMLElement>): HTMLElement {
  const elementoTitulo = document.createElement("h2");
  elementoTitulo.textContent = titulo;
  const cabecalho = document.createElement("div");
  cabecalho.className = "cartao-relatorio__cabecalho";
  cabecalho.append(elementoTitulo);
  const corpo = document.createElement("div");
  corpo.append(elementoCarregando("Carregando..."));
  const cartao = document.createElement("section");
  cartao.className = "cartao-relatorio";
  cartao.append(cabecalho, corpo);
  carregar()
    .then((conteudo) => corpo.replaceChildren(conteudo))
    .catch(() => corpo.replaceChildren(cartaoEstado("Não foi possível carregar.", "erro")));
  return cartao;
}

async function carregarEstoqueBaixo(): Promise<HTMLElement> {
  const baixos = (await listarProdutos())
    .filter((produto) => produto.ativo && situacaoEstoque(produto.quantidadeEmEstoque, produto.estoqueMinimo).modificador !== "em_estoque")
    .sort((a, b) => a.quantidadeEmEstoque - b.quantidadeEmEstoque)
    .slice(0, ESTOQUE_BAIXO_MAXIMO);
  if (baixos.length === 0) {
    return cartaoEstado("Nenhum produto com estoque baixo.");
  }
  const lista = document.createElement("ul");
  lista.className = "lista-simples";
  lista.append(...baixos.map((produto) => {
    const situacao = situacaoEstoque(produto.quantidadeEmEstoque, produto.estoqueMinimo);
    const nome = document.createElement("span");
    nome.textContent = produto.nome;
    const selo = document.createElement("span");
    selo.className = `selo selo--${situacao.modificador}`;
    selo.textContent = `${produto.quantidadeEmEstoque} ${produto.unidadeMedida}`;
    const item = document.createElement("li");
    item.append(nome, selo);
    return item;
  }));
  return lista;
}

async function carregarPedidosPorStatus(): Promise<HTMLElement> {
  const porStatus = await consultarPedidosPorStatus();
  const fatias: Fatia[] = Object.entries(porStatus).map(([status, quantidade]) => ({
    rotulo: STATUS_PEDIDO[status]?.rotulo ?? status,
    cor: STATUS_PEDIDO[status]?.cor ?? COR_STATUS_DESCONHECIDO,
    valor: quantidade,
    detalhe: "",
  }));
  if (!fatias.some((fatia) => fatia.valor > 0)) {
    return cartaoEstado("Nenhum pedido registrado.");
  }
  return criarGraficoRosca({
    fatias, formatarValor: formatarInteiro, formatarTotal: formatarInteiro, rotuloTotal: "pedidos",
    descricao: "Pedidos por situação",
  });
}

async function carregarCobrancasPendentes(): Promise<HTMLElement> {
  const pendentes = await consultarCobrancasPendentes();
  if (pendentes.length === 0) {
    return cartaoEstado("Nenhum recebimento pendente — tudo em dia.");
  }
  const total = pendentes.reduce((soma, cobranca) => soma + cobranca.valor, 0);
  const resumo = document.createElement("p");
  resumo.className = "painel-operacional__resumo";
  resumo.textContent = `${formatarInteiro(pendentes.length)} recebimento(s) · ${formatarMoeda(total)}`;
  return resumo;
}

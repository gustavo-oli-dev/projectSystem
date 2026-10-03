import {
  consultarCobrancasPendentes,
  consultarFaturamento,
  consultarPedidosPorStatus,
  type ResumoCobranca,
} from "../../api/painelApi.js";
import { possui } from "../../state/sessaoState.js";
import { cartaoEstado } from "../estadoCard.js";
import { formatarMoeda } from "../formatarMoeda.js";

/**
 * Cada widget só é carregado se o cargo enxerga aquela informação — um gerente pode ver o painel
 * sem ver o faturamento. Falha em um widget não derruba os outros.
 */
export async function montarPainel(container: HTMLElement): Promise<void> {
  const titulo = document.createElement("h1");
  titulo.textContent = "Painel";

  const grade = document.createElement("div");
  grade.className = "grade-widgets";

  const secaoCobrancas = document.createElement("section");
  secaoCobrancas.className = "secao-painel";

  const carregamentos: Promise<void>[] = [];
  if (possui("FATURAMENTO_VER")) {
    carregamentos.push(preencherWidget(grade, carregarWidgetFaturamento));
  }
  if (possui("PEDIDOS_VER")) {
    carregamentos.push(preencherWidget(grade, carregarWidgetPedidos));
  }
  if (possui("COBRANCAS_VER")) {
    carregamentos.push(preencherWidget(grade, () => carregarWidgetCobrancas(secaoCobrancas)));
  }

  if (carregamentos.length === 0) {
    container.replaceChildren(titulo, cartaoEstado("Seu perfil de acesso não inclui nenhum indicador do painel."));
    return;
  }

  container.replaceChildren(titulo, grade, secaoCobrancas);
  await Promise.all(carregamentos);
}

async function preencherWidget(grade: HTMLElement, carregar: () => Promise<HTMLElement>): Promise<void> {
  const espaco = widgetCarregando();
  grade.append(espaco);
  try {
    espaco.replaceWith(await carregar());
  } catch {
    espaco.replaceWith(widget("Indisponível", "—", "Não foi possível carregar este número."));
  }
}

async function carregarWidgetFaturamento(): Promise<HTMLElement> {
  const hoje = new Date();
  const primeiroDiaDoMes = new Date(hoje.getFullYear(), hoje.getMonth(), 1);
  const faturamento = await consultarFaturamento(formatarData(primeiroDiaDoMes), formatarData(hoje));
  return widget(
    "Faturamento do mês",
    formatarMoeda(faturamento.total),
    `${faturamento.quantidadeCobrancas} cobrança(s) paga(s)`,
    true
  );
}

async function carregarWidgetPedidos(): Promise<HTMLElement> {
  const pedidosPorStatus = await consultarPedidosPorStatus();
  const pedidosEmAberto = pedidosPorStatus.ABERTO + pedidosPorStatus.AGUARDANDO_EMISSAO;
  return widget("Pedidos em aberto", String(pedidosEmAberto), `${pedidosPorStatus.CONCLUIDO} concluído(s)`);
}

async function carregarWidgetCobrancas(secaoCobrancas: HTMLElement): Promise<HTMLElement> {
  const cobrancasPendentes = await consultarCobrancasPendentes();
  renderizarCobrancasPendentes(secaoCobrancas, cobrancasPendentes);
  return widget("Cobranças pendentes", String(cobrancasPendentes.length), somaPendentes(cobrancasPendentes));
}

function renderizarCobrancasPendentes(secao: HTMLElement, cobrancas: ResumoCobranca[]): void {
  const tituloSecao = document.createElement("h2");
  tituloSecao.className = "secao-painel__titulo";
  tituloSecao.textContent = "Cobranças pendentes";
  secao.replaceChildren(tituloSecao);

  if (cobrancas.length === 0) {
    secao.append(cartaoEstado("Nenhuma cobrança pendente — tudo em dia."));
    return;
  }

  const lista = document.createElement("div");
  lista.className = "lista-resumo";
  for (const cobranca of cobrancas.slice(0, 6)) {
    lista.append(criarItemCobranca(cobranca));
  }
  secao.append(lista);
}

function criarItemCobranca(cobranca: ResumoCobranca): HTMLElement {
  const linha = document.createElement("div");
  linha.className = "lista-resumo__item";

  const meio = document.createElement("span");
  meio.textContent = cobranca.meio === "PIX" ? "Pix" : "Boleto";

  const valor = document.createElement("span");
  valor.className = "lista-resumo__valor";
  valor.textContent = formatarMoeda(cobranca.valor);

  linha.append(meio, valor);
  return linha;
}

function somaPendentes(cobrancas: ResumoCobranca[]): string {
  const total = cobrancas.reduce((soma, cobranca) => soma + cobranca.valor, 0);
  return `${formatarMoeda(total)} no total`;
}

function widget(rotulo: string, valor: string, detalhe: string, destaque = false): HTMLElement {
  const card = document.createElement("div");
  card.className = destaque ? "widget widget--destaque" : "widget";

  const rotuloEl = document.createElement("p");
  rotuloEl.className = "widget__rotulo";
  rotuloEl.textContent = rotulo;

  const valorEl = document.createElement("p");
  valorEl.className = "widget__valor";
  valorEl.textContent = valor;

  const detalheEl = document.createElement("p");
  detalheEl.className = "widget__detalhe";
  detalheEl.textContent = detalhe;

  card.append(rotuloEl, valorEl, detalheEl);
  return card;
}

function widgetCarregando(): HTMLElement {
  const card = document.createElement("div");
  card.className = "widget widget--carregando";
  return card;
}

function formatarData(data: Date): string {
  const ano = data.getFullYear();
  const mes = String(data.getMonth() + 1).padStart(2, "0");
  const dia = String(data.getDate()).padStart(2, "0");
  return `${ano}-${mes}-${dia}`;
}

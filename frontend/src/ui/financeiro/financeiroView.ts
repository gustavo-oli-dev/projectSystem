import { baixarLancamentos, gerarFluxoDeCaixa, type FluxoDeCaixa } from "../../api/financeiroApi.js";
import type { Periodo } from "../../api/relatoriosApi.js";
import { criarCampoSelecao } from "../camposFormulario.js";
import { elementoCarregando } from "../estadoCarregamento.js";
import { cartaoEstado } from "../estadoCard.js";
import { formatarMoeda } from "../formatarMoeda.js";
import { ATALHOS, deIso, periodoDoAtalho, type AtalhoPeriodo } from "../painel/periodoPainel.js";
import { criarCartao } from "../painel/secoesRelatorio.js";
import { celula, criarLinha, criarTabela } from "../tabela.js";

const DATA = new Intl.DateTimeFormat("pt-BR", { dateStyle: "short" });
const ATALHO_INICIAL: AtalhoPeriodo = "MES";
const ROTULO_FORMA: Record<string, string> = {
  DINHEIRO: "Dinheiro",
  CARTAO_CREDITO: "Crédito",
  CARTAO_DEBITO: "Débito",
  PIX: "Pix (maquininha)",
  PIX_ONLINE: "Pix (QR ou online)",
  BOLETO_ONLINE: "Boleto",
};

/**
 * Financeiro (D40): o que entrou (vendas recebidas), o que saiu (contas pagas) e o saldo, dia a
 * dia, com exportação dos lançamentos para o Excel.
 */
export async function montarFinanceiro(container: HTMLElement): Promise<void> {
  const titulo = document.createElement("h1");
  titulo.textContent = "Financeiro";
  const periodo = criarCampoSelecao("financeiro-periodo", "Período",
    ATALHOS.map((opcao) => ({ valor: opcao.atalho, rotulo: opcao.rotulo })));
  periodo.selecao.value = ATALHO_INICIAL;
  periodo.container.classList.add("financeiro__periodo");
  const exportar = document.createElement("button");
  exportar.type = "button";
  exportar.className = "btn btn-ghost";
  exportar.textContent = "Exportar lançamentos (CSV)";
  const acoes = document.createElement("div");
  acoes.className = "financeiro__acoes";
  acoes.append(periodo.container, exportar);
  const cabecalho = document.createElement("div");
  cabecalho.className = "cabecalho-pagina";
  cabecalho.append(titulo, acoes);
  const area = document.createElement("div");
  area.className = "financeiro";
  container.replaceChildren(cabecalho, area);

  // Valor vem de uma lista fixa de opções (ATALHOS), então sempre é um atalho válido.
  const periodoAtual = (): Periodo => periodoDoAtalho(periodo.selecao.value as AtalhoPeriodo);
  const carregar = async (): Promise<void> => {
    area.replaceChildren(elementoCarregando("Carregando o financeiro..."));
    try {
      area.replaceChildren(...montarRelatorio(await gerarFluxoDeCaixa(periodoAtual())));
    } catch {
      area.replaceChildren(cartaoEstado("Não foi possível carregar o financeiro.", "erro"));
    }
  };
  periodo.selecao.addEventListener("change", () => void carregar());
  exportar.addEventListener("click", () => {
    const escolhido = periodoAtual();
    exportar.disabled = true;
    baixarLancamentos(escolhido)
      .then((arquivo) => salvarArquivo(arquivo, `financeiro_${escolhido.inicio}_a_${escolhido.fim}.csv`))
      .catch(() => window.alert("Não foi possível exportar os lançamentos."))
      .finally(() => {
        exportar.disabled = false;
      });
  });
  await carregar();
}

function montarRelatorio(fluxo: FluxoDeCaixa): HTMLElement[] {
  if (fluxo.porDia.length === 0 && fluxo.aPagarNoPeriodo === 0) {
    return [cartaoEstado("Nenhuma entrada nem saída neste período.")];
  }
  return [criarIndicadores(fluxo), criarPorForma(fluxo), criarPorDia(fluxo), criarSaidas(fluxo)];
}

function criarIndicadores(fluxo: FluxoDeCaixa): HTMLElement {
  const indicadores: Array<[string, string, string]> = [
    ["Entrou", formatarMoeda(fluxo.totalEntradas), "vendas recebidas no período"],
    ["Saiu", formatarMoeda(fluxo.totalSaidas), "contas pagas no período"],
    ["Saldo", formatarSaldo(fluxo.saldo), "entrou − saiu"],
    ["A pagar", formatarMoeda(fluxo.aPagarNoPeriodo), "contas abertas que vencem no período"],
  ];
  const grade = document.createElement("div");
  grade.className = "indicadores";
  grade.append(...indicadores.map(([rotulo, valor, nota]) => {
    const elementoRotulo = document.createElement("p");
    elementoRotulo.className = "indicador__rotulo";
    elementoRotulo.textContent = rotulo;
    const elementoValor = document.createElement("p");
    elementoValor.className = "indicador__valor";
    elementoValor.textContent = valor;
    const elementoNota = document.createElement("p");
    elementoNota.className = "indicador__nota";
    elementoNota.textContent = nota;
    const cartao = document.createElement("article");
    cartao.className = "indicador";
    cartao.append(elementoRotulo, elementoValor, elementoNota);
    return cartao;
  }));
  return grade;
}

function criarPorForma(fluxo: FluxoDeCaixa): HTMLElement {
  const formas = Object.entries(fluxo.entradasPorForma).sort(([, primeiro], [, segundo]) => segundo - primeiro);
  return criarCartao("Entradas por forma de recebimento", formas.length === 0
    ? cartaoEstado("Nenhuma venda recebida no período.")
    : criarTabela(["Forma", "Valor"], formas.map(([forma, valor]) => criarLinha(
      celula(ROTULO_FORMA[forma] ?? forma), celula(formatarMoeda(valor))
    )), "forma(s)"));
}

function criarPorDia(fluxo: FluxoDeCaixa): HTMLElement {
  return criarCartao("Dia a dia", criarTabela(["Dia", "Entrou", "Saiu", "Saldo do dia"], fluxo.porDia.map((dia) => criarLinha(
    celula(DATA.format(deIso(dia.dia))), celula(formatarMoeda(dia.entradas)), celula(formatarMoeda(dia.saidas)),
    celula(formatarSaldo(dia.saldo))
  )), "dia(s)"));
}

function criarSaidas(fluxo: FluxoDeCaixa): HTMLElement {
  return criarCartao("Contas pagas", fluxo.saidas.length === 0
    ? cartaoEstado("Nenhuma conta paga no período.")
    : criarTabela(["Dia", "Descrição", "Contato", "Valor"], fluxo.saidas.map((saida) => criarLinha(
      celula(DATA.format(deIso(saida.dia))), celula(saida.descricao), celula(saida.contato ?? "—"),
      celula(formatarMoeda(saida.valor))
    )), "conta(s)"));
}

/** "+ R$ 5,00" ou "− R$ 10,00". */
function formatarSaldo(saldo: number): string {
  if (saldo === 0) {
    return formatarMoeda(0);
  }
  return saldo > 0 ? `+ ${formatarMoeda(saldo)}` : `− ${formatarMoeda(-saldo)}`;
}

function salvarArquivo(arquivo: Blob, nome: string): void {
  const endereco = URL.createObjectURL(arquivo);
  const link = document.createElement("a");
  link.href = endereco;
  link.download = nome;
  link.click();
  URL.revokeObjectURL(endereco);
}

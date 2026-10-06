import type { FluxoDeCaixa } from "../../api/financeiroApi.js";
import { cartaoEstado } from "../estadoCard.js";
import { formatarMoeda } from "../formatarMoeda.js";
import { celula, criarLinha, criarTabela } from "../tabela.js";
import { deIso } from "./periodoPainel.js";
import { criarCartao } from "./secoesRelatorio.js";

const DATA = new Intl.DateTimeFormat("pt-BR", { dateStyle: "short" });
const ROTULO_FORMA: Record<string, string> = {
  DINHEIRO: "Dinheiro",
  CARTAO_CREDITO: "Crédito",
  CARTAO_DEBITO: "Débito",
  PIX: "Pix (maquininha)",
  PIX_ONLINE: "Pix (QR ou online)",
  BOLETO_ONLINE: "Boleto",
};

/**
 * Aba Financeiro do Painel (D40): o que entrou (vendas recebidas), o que saiu (contas pagas) e o
 * saldo no período do filtro, dia a dia. Só aparece para quem tem a permissão Financeiro.
 */
export function montarAbaFinanceiro(fluxo: FluxoDeCaixa, acaoExportar: HTMLElement): HTMLElement[] {
  if (fluxo.porDia.length === 0 && fluxo.aPagarNoPeriodo === 0) {
    return [cartaoEstado("Nenhuma entrada nem saída neste período.")];
  }
  return [criarIndicadores(fluxo), criarPorForma(fluxo), criarPorDia(fluxo, acaoExportar), criarSaidas(fluxo)];
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

function criarPorDia(fluxo: FluxoDeCaixa, acaoExportar: HTMLElement): HTMLElement {
  const cartao = criarCartao("Dia a dia", criarTabela(["Dia", "Entrou", "Saiu", "Saldo do dia"], fluxo.porDia.map((dia) => criarLinha(
    celula(DATA.format(deIso(dia.dia))), celula(formatarMoeda(dia.entradas)), celula(formatarMoeda(dia.saidas)),
    celula(formatarSaldo(dia.saldo))
  )), "dia(s)"));
  cartao.querySelector(".cartao-relatorio__cabecalho")?.append(acaoExportar);
  return cartao;
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

import type { Granularidade, PontoSerie, Recorte, RelatorioVendas } from "../../api/relatoriosApi.js";
import { cartaoEstado } from "../estadoCard.js";
import { formatarMoeda } from "../formatarMoeda.js";
import { formatarInteiro, formatarMoedaCompacta } from "../formatarNumero.js";
import { criarGraficoColunas, type Coluna } from "../graficos/graficoColunas.js";
import { criarGraficoRosca, type CorFatia, type Fatia } from "../graficos/graficoRosca.js";
import { celula, criarLinha, criarTabela } from "../tabela.js";
import { deIso } from "./periodoPainel.js";

const TITULO_SERIE: Record<Granularidade, string> = {
  DIA: "Faturamento por dia",
  SEMANA: "Faturamento por semana",
  MES: "Faturamento por mês",
};
/** Rótulo e cor fixos por categoria: a cor acompanha a categoria em qualquer período. */
const CANAIS: Record<string, { rotulo: string; cor: CorFatia }> = {
  BALCAO: { rotulo: "Balcão (caixa)", cor: 1 },
  PAINEL: { rotulo: "Painel / online", cor: 2 },
};
const FORMAS: Record<string, { rotulo: string; cor: CorFatia }> = {
  DINHEIRO: { rotulo: "Dinheiro", cor: 1 },
  CARTAO_CREDITO: { rotulo: "Cartão de crédito", cor: 2 },
  CARTAO_DEBITO: { rotulo: "Cartão de débito", cor: 3 },
  PIX: { rotulo: "Pix na maquininha", cor: 4 },
  PIX_ONLINE: { rotulo: "Pix (QR / online)", cor: 5 },
  BOLETO_ONLINE: { rotulo: "Boleto", cor: 6 },
};
const COR_CANAL_DESCONHECIDO: CorFatia = 2;
const COR_FORMA_DESCONHECIDA: CorFatia = 6;
const COR_CUSTO: CorFatia = 1;
const COR_LUCRO: CorFatia = 3;
const DIAS_DA_SEMANA = ["Seg", "Ter", "Qua", "Qui", "Sex", "Sáb", "Dom"];
const DIAS_DA_SEMANA_COMPLETOS = ["Segunda", "Terça", "Quarta", "Quinta", "Sexta", "Sábado", "Domingo"];
const HORAS_DO_DIA = 24;

const CURTO = new Intl.DateTimeFormat("pt-BR", { day: "2-digit", month: "2-digit" });
const LONGO = new Intl.DateTimeFormat("pt-BR", { weekday: "long", day: "numeric", month: "long" });
const MES_CURTO = new Intl.DateTimeFormat("pt-BR", { month: "short", year: "2-digit" });
const MES_LONGO = new Intl.DateTimeFormat("pt-BR", { month: "long", year: "numeric" });

export function criarSecaoFaturamento(relatorio: RelatorioVendas): HTMLElement {
  const colunas: Coluna[] = relatorio.serie.map((ponto) => ({
    ...rotulosDoPonto(ponto, relatorio.granularidade),
    valor: ponto.faturamento,
    detalhes: [
      ["Vendas", formatarInteiro(ponto.vendas)],
      ["Lucro bruto", formatarMoeda(ponto.lucroBruto)],
    ],
  }));
  const titulo = TITULO_SERIE[relatorio.granularidade];
  const grafico = criarGraficoColunas({
    colunas, formatarValor: formatarMoeda, formatarEixo: formatarMoedaCompacta, descricao: titulo, alturaPlot: 260,
  });
  const tabela = criarTabela(["Período", "Vendas", "Faturamento", "Lucro bruto"], relatorio.serie.map((ponto) => criarLinha(
    celula(rotulosDoPonto(ponto, relatorio.granularidade).rotuloCompleto),
    celula(formatarInteiro(ponto.vendas)),
    celula(formatarMoeda(ponto.faturamento)),
    celula(formatarMoeda(ponto.lucroBruto))
  )), "período(s)");
  return criarCartao(titulo, grafico, tabela);
}

export function criarSecaoCanais(relatorio: RelatorioVendas): HTMLElement {
  const fatias: Fatia[] = relatorio.porCanal.map((recorte) => ({
    rotulo: CANAIS[recorte.chave]?.rotulo ?? recorte.chave,
    cor: CANAIS[recorte.chave]?.cor ?? COR_CANAL_DESCONHECIDO,
    valor: recorte.valor,
    detalhe: `${formatarInteiro(recorte.vendas)} venda(s)`,
  }));
  return criarCartao("Vendas por canal", roscaDeFaturamento(fatias, "Faturamento por canal de venda"));
}

export function criarSecaoFormasPagamento(relatorio: RelatorioVendas): HTMLElement {
  const fatias: Fatia[] = relatorio.porFormaPagamento.map((recorte) => ({
    rotulo: FORMAS[recorte.chave]?.rotulo ?? recorte.chave,
    cor: FORMAS[recorte.chave]?.cor ?? COR_FORMA_DESCONHECIDA,
    valor: recorte.valor,
    detalhe: `${formatarInteiro(recorte.vendas)} pagamento(s)`,
  }));
  fatias.push({ rotulo: "A receber", cor: "restante", valor: relatorio.aReceber, detalhe: "ainda sem pagamento" });
  return criarCartao("Formas de pagamento", roscaDeFaturamento(fatias, "Faturamento por forma de pagamento"));
}

/**
 * Para onde vai cada real vendido: custo dos itens × lucro bruto. A parte vendida sem custo
 * cadastrado aparece separada, em vez de inflar o lucro.
 */
export function criarSecaoCustoLucro(relatorio: RelatorioVendas): HTMLElement {
  const { faturamento, lucroBruto, margem, coberturaCusto } = relatorio.resumo;
  const titulo = "Custo × lucro";
  if (margem === null) {
    return criarCartao(titulo, cartaoEstado("Cadastre o custo dos produtos para ver quanto das vendas é lucro."));
  }
  if (lucroBruto < 0) {
    return criarCartao(titulo, cartaoEstado(`Prejuízo de ${formatarMoeda(-lucroBruto)}: vendas abaixo do custo.`, "erro"));
  }
  const vendidoComCusto = faturamento * coberturaCusto;
  const fatias: Fatia[] = [
    { rotulo: "Custo", cor: COR_CUSTO, valor: vendidoComCusto - lucroBruto, detalhe: "o que os itens custaram" },
    { rotulo: "Lucro bruto", cor: COR_LUCRO, valor: lucroBruto, detalhe: "o que sobrou" },
    { rotulo: "Sem custo cadastrado", cor: "restante", valor: faturamento - vendidoComCusto, detalhe: "fora do cálculo" },
  ];
  return criarCartao(titulo, roscaDeFaturamento(fatias, "Faturamento dividido entre custo e lucro bruto"));
}

function roscaDeFaturamento(fatias: readonly Fatia[], descricao: string): HTMLElement {
  if (!fatias.some((fatia) => fatia.valor > 0)) {
    return semDados();
  }
  return criarGraficoRosca({
    fatias, descricao, formatarValor: formatarMoeda, formatarTotal: formatarMoedaCompacta, rotuloTotal: "no período",
  });
}

export function criarSecaoHorarios(relatorio: RelatorioVendas): HTMLElement {
  const porHora = completar(relatorio.porHora, HORAS_DO_DIA, (indice) => String(indice));
  const colunas: Coluna[] = porHora.map((recorte, hora) => ({
    rotulo: `${String(hora).padStart(2, "0")}h`,
    rotuloCompleto: `Das ${String(hora).padStart(2, "0")}h às ${String(hora).padStart(2, "0")}h59`,
    valor: recorte.vendas,
    detalhes: [["Faturamento", formatarMoeda(recorte.valor)]],
  }));
  const grafico = criarGraficoColunas({
    colunas, formatarValor: (valor) => `${formatarInteiro(valor)} venda(s)`, formatarEixo: formatarInteiro,
    descricao: "Vendas por horário do dia", alturaPlot: 180,
  });
  return criarCartao("Horário de pico", grafico, tabelaDeRecortes(porHora, (indice) => `${String(indice).padStart(2, "0")}h`));
}

export function criarSecaoDiasDaSemana(relatorio: RelatorioVendas): HTMLElement {
  const porDia = completar(relatorio.porDiaDaSemana, DIAS_DA_SEMANA.length, (indice) => String(indice + 1));
  const colunas: Coluna[] = porDia.map((recorte, indice) => ({
    rotulo: DIAS_DA_SEMANA[indice] ?? "",
    rotuloCompleto: DIAS_DA_SEMANA_COMPLETOS[indice] ?? "",
    valor: recorte.vendas,
    detalhes: [["Faturamento", formatarMoeda(recorte.valor)]],
  }));
  const grafico = criarGraficoColunas({
    colunas, formatarValor: (valor) => `${formatarInteiro(valor)} venda(s)`, formatarEixo: formatarInteiro,
    descricao: "Vendas por dia da semana", alturaPlot: 180,
  });
  return criarCartao("Dias da semana", grafico, tabelaDeRecortes(porDia, (indice) => DIAS_DA_SEMANA_COMPLETOS[indice] ?? ""));
}

export function criarSecaoMaisVendidos(relatorio: RelatorioVendas, acaoExportar: HTMLElement): HTMLElement {
  if (relatorio.maisVendidos.length === 0) {
    return criarCartao("Mais vendidos", semDados());
  }
  const tabela = criarTabela(
    ["#", "Item", "Tipo", "Unidades", "Faturamento", "Lucro bruto"],
    relatorio.maisVendidos.map((item, posicao) => criarLinha(
      celula(String(posicao + 1)),
      celula(item.descricao),
      celula(item.tipo === "PRODUTO" ? "Produto" : "Serviço"),
      celula(formatarInteiro(item.unidades)),
      celula(formatarMoeda(item.faturamento)),
      celula(textoDoLucro(item.lucroBruto, item.custoCompleto))
    )),
    "item(ns)"
  );
  const cartao = criarCartao("Mais vendidos", tabela);
  cartao.querySelector(".cartao-relatorio__cabecalho")?.append(acaoExportar);
  return cartao;
}

/** Lucro parcial (parte das vendas sem custo) aparece marcado, em vez de sumir. */
function textoDoLucro(lucro: number | null, completo: boolean): string {
  if (lucro === null) {
    return "Custo não informado";
  }
  return completo ? formatarMoeda(lucro) : `${formatarMoeda(lucro)} (parcial)`;
}

function rotulosDoPonto(ponto: PontoSerie, granularidade: Granularidade): Pick<Coluna, "rotulo" | "rotuloCompleto"> {
  const data = deIso(ponto.inicio);
  if (granularidade === "MES") {
    return { rotulo: MES_CURTO.format(data), rotuloCompleto: MES_LONGO.format(data) };
  }
  if (granularidade === "SEMANA") {
    return { rotulo: CURTO.format(data), rotuloCompleto: `Semana de ${CURTO.format(data)}` };
  }
  return { rotulo: CURTO.format(data), rotuloCompleto: LONGO.format(data) };
}

/** Horas/dias sem venda entram com zero (o gráfico mostra o dia inteiro, não só onde houve venda). */
function completar(recortes: readonly Recorte[], quantidade: number, chaveDoIndice: (indice: number) => string): Recorte[] {
  const porChave = new Map(recortes.map((recorte) => [recorte.chave, recorte] as const));
  return Array.from({ length: quantidade }, (_, indice) => {
    const chave = chaveDoIndice(indice);
    return porChave.get(chave) ?? { chave, vendas: 0, valor: 0 };
  });
}

function tabelaDeRecortes(recortes: readonly Recorte[], rotulo: (indice: number) => string): HTMLElement {
  return criarTabela(["Quando", "Vendas", "Faturamento"], recortes.map((recorte, indice) => criarLinha(
    celula(rotulo(indice)), celula(formatarInteiro(recorte.vendas)), celula(formatarMoeda(recorte.valor))
  )), "linha(s)");
}

function semDados(): HTMLElement {
  return cartaoEstado("Sem vendas neste período.");
}

/** Cartão com título; se houver tabela, ela fica num "Ver em tabela" (o gráfico nunca é o único acesso ao número). */
export function criarCartao(titulo: string, conteudo: HTMLElement, tabela?: HTMLElement): HTMLElement {
  const cabecalho = document.createElement("div");
  cabecalho.className = "cartao-relatorio__cabecalho";
  const elementoTitulo = document.createElement("h2");
  elementoTitulo.textContent = titulo;
  cabecalho.append(elementoTitulo);

  const cartao = document.createElement("section");
  cartao.className = "cartao-relatorio";
  cartao.append(cabecalho, conteudo);

  if (tabela !== undefined) {
    const resumo = document.createElement("summary");
    resumo.textContent = "Ver em tabela";
    const detalhes = document.createElement("details");
    detalhes.className = "cartao-relatorio__tabela";
    detalhes.append(resumo, tabela);
    cartao.append(detalhes);
  }
  return cartao;
}

import type { RelatorioCaixa, ResultadoFechamento } from "../../api/relatoriosApi.js";
import { cartaoEstado } from "../estadoCard.js";
import { formatarMoeda } from "../formatarMoeda.js";
import { formatarInteiro } from "../formatarNumero.js";
import { criarGraficoRosca, type CorFatia, type Fatia } from "../graficos/graficoRosca.js";
import { celula, celulaSelo, criarLinha, criarTabela } from "../tabela.js";
import { criarCartao } from "./secoesRelatorio.js";

const DATA_HORA = new Intl.DateTimeFormat("pt-BR", { day: "2-digit", month: "2-digit", hour: "2-digit", minute: "2-digit" });
/** Slots validados juntos (pares adjacentes do anel); a cor segue o resultado, nunca a posição. */
const FATIAS_RESULTADO: ReadonlyArray<{ resultado: ResultadoFechamento; rotulo: string; cor: CorFatia }> = [
  { resultado: "SOBROU", rotulo: "Sobrou", cor: 1 },
  { resultado: "FALTOU", rotulo: "Faltou", cor: 2 },
  { resultado: "BATEU", rotulo: "Bateu", cor: 3 },
];

/** Aba Caixa do painel: números da gestão de caixa no período (D28). */
export function montarAbaCaixa(relatorio: RelatorioCaixa, acaoExportar: HTMLElement): HTMLElement[] {
  if (relatorio.totais.caixas === 0) {
    return [cartaoEstado("Nenhum caixa foi aberto neste período.")];
  }
  const porOperador = criarSecaoPorOperador(relatorio);
  porOperador.classList.add("cartao-relatorio--largo");
  porOperador.querySelector(".cartao-relatorio__cabecalho")?.append(acaoExportar);
  const linha = document.createElement("div");
  linha.className = "linha-relatorio";
  linha.append(criarSecaoResultado(relatorio), porOperador);
  return [criarIndicadoresCaixa(relatorio), linha, criarSecaoCaixas(relatorio)];
}

function criarIndicadoresCaixa(relatorio: RelatorioCaixa): HTMLElement {
  const { totais } = relatorio;
  const quantidade = (resultado: ResultadoFechamento): number =>
    relatorio.porResultado.find((item) => item.resultado === resultado)?.caixas ?? 0;
  const abertos = totais.caixas - totais.fechados;
  const indicadores: Array<[string, string, string]> = [
    ["Caixas no período", formatarInteiro(totais.caixas),
      `${formatarInteiro(totais.fechados)} fechado(s) · ${formatarInteiro(abertos)} ainda aberto(s)`],
    ["Faltas", formatarMoeda(totais.faltas), `em ${formatarInteiro(quantidade("FALTOU"))} caixa(s)`],
    ["Sobras", formatarMoeda(totais.sobras),
      `em ${formatarInteiro(quantidade("SOBROU"))} caixa(s) · saldo ${formatarSaldo(totais.saldo)}`],
    ["Sangrias", formatarMoeda(totais.sangrias), `Reposições de troco: ${formatarMoeda(totais.reposicoes)}`],
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

function criarSecaoResultado(relatorio: RelatorioCaixa): HTMLElement {
  const fatias: Fatia[] = FATIAS_RESULTADO.map(({ resultado, rotulo, cor }) => ({
    rotulo,
    cor,
    valor: relatorio.porResultado.find((item) => item.resultado === resultado)?.caixas ?? 0,
    detalhe: "",
  }));
  const conteudo = fatias.some((fatia) => fatia.valor > 0)
    ? criarGraficoRosca({
      fatias, formatarValor: formatarInteiro, formatarTotal: formatarInteiro, rotuloTotal: "fechamentos",
      descricao: "Resultado dos fechamentos de caixa",
    })
    : cartaoEstado("Nenhum caixa fechado neste período.");
  return criarCartao("Resultado dos fechamentos", conteudo);
}

function criarSecaoPorOperador(relatorio: RelatorioCaixa): HTMLElement {
  const tabela = criarTabela(
    ["Operador", "Caixas", "Com falta", "Faltas", "Sobras", "Saldo", "Sangrias", "Reposições"],
    relatorio.porOperador.map((operador) => criarLinha(
      celula(operador.operadorNome),
      celula(formatarInteiro(operador.caixas)),
      celula(formatarInteiro(operador.comFalta)),
      celula(formatarMoeda(operador.faltas)),
      celula(formatarMoeda(operador.sobras)),
      celula(formatarSaldo(operador.saldo)),
      celula(formatarMoeda(operador.sangrias)),
      celula(formatarMoeda(operador.reposicoes))
    )),
    "operador(es)"
  );
  return criarCartao("Por operador", tabela);
}

function criarSecaoCaixas(relatorio: RelatorioCaixa): HTMLElement {
  const tabela = criarTabela(
    ["Operador", "Abertura", "Fechamento", "Fundo", "Vendas em dinheiro", "Reposições", "Sangrias", "Esperado", "Contado", "Resultado"],
    relatorio.caixas.map((caixa) => criarLinha(
      celula(caixa.operadorNome),
      celula(DATA_HORA.format(new Date(caixa.abertaEm))),
      celula(caixa.fechadaEm === null ? "—" : DATA_HORA.format(new Date(caixa.fechadaEm))),
      celula(formatarMoeda(caixa.fundoInicial)),
      celula(valorOuTraco(caixa.vendasEmDinheiro)),
      celula(formatarMoeda(caixa.reposicoes)),
      celula(formatarMoeda(caixa.sangrias)),
      celula(valorOuTraco(caixa.valorEsperado)),
      celula(valorOuTraco(caixa.valorContado)),
      celulaResultado(caixa.resultado, caixa.diferenca)
    )),
    "caixa(s)"
  );
  return criarCartao("Caixas do período", tabela);
}

function celulaResultado(resultado: ResultadoFechamento, diferenca: number | null): HTMLTableCellElement {
  if (resultado === "ABERTO" || diferenca === null) {
    return celulaSelo("Aberto", "aberta");
  }
  if (resultado === "BATEU") {
    return celulaSelo("Bateu", "concluido");
  }
  return resultado === "SOBROU"
    ? celulaSelo(`Sobrou ${formatarMoeda(diferenca)}`, "pendente")
    : celulaSelo(`Faltou ${formatarMoeda(-diferenca)}`, "rejeitado");
}

/** Saldo com sinal explícito: "+ R$ 5,00" sobrou, "− R$ 10,00" faltou. */
function formatarSaldo(saldo: number): string {
  if (saldo === 0) {
    return formatarMoeda(0);
  }
  return saldo > 0 ? `+ ${formatarMoeda(saldo)}` : `− ${formatarMoeda(-saldo)}`;
}

function valorOuTraco(valor: number | null): string {
  return valor === null ? "—" : formatarMoeda(valor);
}

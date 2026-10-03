import type { RelatorioVendas } from "../../api/relatoriosApi.js";
import { formatarMoeda } from "../formatarMoeda.js";
import { formatarInteiro, formatarPercentual } from "../formatarNumero.js";

interface Indicador {
  rotulo: string;
  valor: string;
  atual: number;
  anterior: number;
  /** Para "vendas desfeitas", subir é ruim. */
  subirEhBom: boolean;
  nota?: string;
  destaque?: boolean;
}

/** Bloco de números do topo: cada um com a variação contra o período anterior de mesmo tamanho. */
export function criarIndicadores(relatorio: RelatorioVendas): HTMLElement {
  const { resumo, periodoAnterior, desfeitas } = relatorio;
  const indicadores: Indicador[] = [
    {
      rotulo: "Faturamento", valor: formatarMoeda(resumo.faturamento),
      atual: resumo.faturamento, anterior: periodoAnterior.faturamento, subirEhBom: true, destaque: true,
      nota: `${formatarMoeda(relatorio.recebido)} recebido · ${formatarMoeda(relatorio.aReceber)} a receber`,
    },
    {
      rotulo: "Lucro bruto", valor: formatarMoeda(resumo.lucroBruto),
      atual: resumo.lucroBruto, anterior: periodoAnterior.lucroBruto, subirEhBom: true,
      nota: notaDoLucro(resumo.margem, resumo.coberturaCusto),
    },
    {
      rotulo: "Vendas", valor: formatarInteiro(resumo.vendas),
      atual: resumo.vendas, anterior: periodoAnterior.vendas, subirEhBom: true,
    },
    {
      rotulo: "Ticket médio", valor: formatarMoeda(resumo.ticketMedio),
      atual: resumo.ticketMedio, anterior: periodoAnterior.ticketMedio, subirEhBom: true,
    },
    {
      rotulo: "Unidades vendidas", valor: formatarInteiro(resumo.unidades),
      atual: resumo.unidades, anterior: periodoAnterior.unidades, subirEhBom: true,
    },
    {
      rotulo: "Vendas desfeitas", valor: formatarInteiro(desfeitas.vendas),
      atual: desfeitas.vendas, anterior: Number.NaN, subirEhBom: false,
      nota: `${formatarMoeda(desfeitas.valor)} cancelado ou reembolsado`,
    },
  ];

  const grade = document.createElement("div");
  grade.className = "indicadores";
  grade.append(...indicadores.map(criarIndicador));
  return grade;
}

function notaDoLucro(margem: number | null, cobertura: number): string {
  if (margem === null) {
    return "Cadastre o custo dos produtos para calcular o lucro";
  }
  const margemTexto = `Margem ${formatarPercentual(margem)}`;
  return cobertura < 1
    ? `${margemTexto} · custo informado em ${formatarPercentual(cobertura)} das vendas`
    : margemTexto;
}

function criarIndicador(indicador: Indicador): HTMLElement {
  const rotulo = document.createElement("p");
  rotulo.className = "indicador__rotulo";
  rotulo.textContent = indicador.rotulo;

  const valor = document.createElement("p");
  valor.className = "indicador__valor";
  valor.textContent = indicador.valor;

  const cartao = document.createElement("article");
  cartao.className = indicador.destaque === true ? "indicador indicador--destaque" : "indicador";
  cartao.append(rotulo, valor);

  const variacao = criarVariacao(indicador);
  if (variacao !== null) {
    cartao.append(variacao);
  }
  if (indicador.nota !== undefined) {
    const nota = document.createElement("p");
    nota.className = "indicador__nota";
    nota.textContent = indicador.nota;
    cartao.append(nota);
  }
  return cartao;
}

/** "▲ 12% vs período anterior" — seta + texto + cor (nunca só a cor). */
function criarVariacao(indicador: Indicador): HTMLElement | null {
  if (Number.isNaN(indicador.anterior)) {
    return null;
  }
  const elemento = document.createElement("p");
  elemento.className = "indicador__variacao";
  if (indicador.anterior === 0) {
    elemento.textContent = "— sem base de comparação";
    elemento.title = "O período anterior de mesmo tamanho não teve vendas";
    return elemento;
  }
  const variacao = (indicador.atual - indicador.anterior) / Math.abs(indicador.anterior);
  const subiu = variacao > 0;
  const bom = variacao === 0 ? null : subiu === indicador.subirEhBom;
  elemento.classList.add(bom === null ? "indicador__variacao--neutra"
    : bom ? "indicador__variacao--boa" : "indicador__variacao--ruim");
  const seta = variacao === 0 ? "=" : subiu ? "▲" : "▼";
  elemento.textContent = `${seta} ${formatarPercentual(Math.abs(variacao))} vs período anterior`;
  return elemento;
}

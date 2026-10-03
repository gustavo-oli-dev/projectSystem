import type { RelatorioVendas } from "../../api/relatoriosApi.js";
import { formatarMoeda } from "../formatarMoeda.js";
import { formatarInteiro, formatarPercentual } from "../formatarNumero.js";

interface Indicador {
  rotulo: string;
  valor: string;
  atual: number;
  anterior: number;
  nota: string;
}

/** Bloco de números do topo: cada um com a variação contra o período anterior de mesmo tamanho. */
export function criarIndicadores(relatorio: RelatorioVendas): HTMLElement {
  const { resumo, periodoAnterior, desfeitas } = relatorio;
  // Quatro números, todos do mesmo tamanho; o resto vira nota pequena embaixo de cada um.
  const indicadores: Indicador[] = [
    {
      rotulo: "Faturamento", valor: formatarMoeda(resumo.faturamento),
      atual: resumo.faturamento, anterior: periodoAnterior.faturamento,
      nota: `${formatarMoeda(relatorio.recebido)} recebido · ${formatarMoeda(relatorio.aReceber)} a receber`,
    },
    {
      rotulo: "Lucro bruto", valor: formatarMoeda(resumo.lucroBruto),
      atual: resumo.lucroBruto, anterior: periodoAnterior.lucroBruto,
      nota: notaDoLucro(resumo.margem, resumo.coberturaCusto),
    },
    {
      rotulo: "Vendas", valor: formatarInteiro(resumo.vendas),
      atual: resumo.vendas, anterior: periodoAnterior.vendas,
      nota: `${formatarInteiro(resumo.unidades)} unidade(s) · ${formatarInteiro(desfeitas.vendas)} desfeita(s)`,
    },
    {
      rotulo: "Ticket médio", valor: formatarMoeda(resumo.ticketMedio),
      atual: resumo.ticketMedio, anterior: periodoAnterior.ticketMedio,
      nota: "Valor médio por venda",
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

  const nota = document.createElement("p");
  nota.className = "indicador__nota";
  nota.textContent = indicador.nota;

  const cartao = document.createElement("article");
  cartao.className = "indicador";
  cartao.append(rotulo, valor, criarVariacao(indicador), nota);
  return cartao;
}

/** "▲ 12% vs período anterior" — seta + texto + cor (nunca só a cor). Nos quatro, subir é bom. */
function criarVariacao(indicador: Indicador): HTMLElement {
  const elemento = document.createElement("p");
  elemento.className = "indicador__variacao";
  if (indicador.anterior === 0) {
    elemento.textContent = "— sem base de comparação";
    elemento.title = "O período anterior de mesmo tamanho não teve vendas";
    return elemento;
  }
  const variacao = (indicador.atual - indicador.anterior) / Math.abs(indicador.anterior);
  const subiu = variacao > 0;
  elemento.classList.add(variacao === 0 ? "indicador__variacao--neutra"
    : subiu ? "indicador__variacao--boa" : "indicador__variacao--ruim");
  const seta = variacao === 0 ? "=" : subiu ? "▲" : "▼";
  elemento.textContent = `${seta} ${formatarPercentual(Math.abs(variacao))} vs período anterior`;
  return elemento;
}

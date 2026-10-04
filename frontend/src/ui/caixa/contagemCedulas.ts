import type { Cedula, CedulaContada, Contagem } from "../../api/caixaApi.js";
import { formatarMoeda } from "../formatarMoeda.js";

interface DefinicaoCedula {
  cedula: Cedula;
  rotulo: string;
  /** Em centavos: soma sem erro de ponto flutuante. */
  centavos: number;
}

/** Da maior para a menor: é a ordem em que se conta uma gaveta. */
const NOTAS: readonly DefinicaoCedula[] = [
  { cedula: "NOTA_200", rotulo: "R$ 200", centavos: 20000 },
  { cedula: "NOTA_100", rotulo: "R$ 100", centavos: 10000 },
  { cedula: "NOTA_50", rotulo: "R$ 50", centavos: 5000 },
  { cedula: "NOTA_20", rotulo: "R$ 20", centavos: 2000 },
  { cedula: "NOTA_10", rotulo: "R$ 10", centavos: 1000 },
  { cedula: "NOTA_5", rotulo: "R$ 5", centavos: 500 },
  { cedula: "NOTA_2", rotulo: "R$ 2", centavos: 200 },
];
const MOEDAS: readonly DefinicaoCedula[] = [
  { cedula: "MOEDA_1_REAL", rotulo: "R$ 1", centavos: 100 },
  { cedula: "MOEDA_50_CENTAVOS", rotulo: "50 centavos", centavos: 50 },
  { cedula: "MOEDA_25_CENTAVOS", rotulo: "25 centavos", centavos: 25 },
  { cedula: "MOEDA_10_CENTAVOS", rotulo: "10 centavos", centavos: 10 },
  { cedula: "MOEDA_5_CENTAVOS", rotulo: "5 centavos", centavos: 5 },
];
const CENTAVOS_POR_REAL = 100;
const QUANTIDADE_MAXIMA = 100000;

export const ROTULO_CEDULA: Record<Cedula, string> = Object.fromEntries(
  [...NOTAS, ...MOEDAS].map(({ cedula, rotulo }) => [cedula, rotulo])
) as Record<Cedula, string>;

export interface ContagemCedulasView {
  elemento: HTMLElement;
  contagem: () => Contagem;
  /** Em reais. */
  total: () => number;
  focar: () => void;
}

/** Lista vinda da API → objeto de contagem (para preencher a tela). */
export function contagemDe(cedulas: readonly CedulaContada[]): Contagem {
  const contagem: Contagem = {};
  for (const { cedula, quantidade } of cedulas) {
    contagem[cedula] = quantidade;
  }
  return contagem;
}

/**
 * Quantidade de cada nota e moeda, com subtotal por linha e total geral. O total nunca é digitado:
 * sai da soma das cédulas.
 */
export function criarContagemCedulas(inicial: Contagem, aoMudar?: (total: number) => void): ContagemCedulasView {
  const entradas = new Map<Cedula, HTMLInputElement>();
  const subtotais = new Map<Cedula, HTMLElement>();

  const valorTotal = document.createElement("strong");
  valorTotal.className = "contagem-cedulas__valor-total";

  const centavosTotais = (): number => [...NOTAS, ...MOEDAS]
    .reduce((soma, definicao) => soma + quantidadeDe(entradas.get(definicao.cedula)) * definicao.centavos, 0);

  const atualizar = (): void => {
    for (const definicao of [...NOTAS, ...MOEDAS]) {
      const subtotal = subtotais.get(definicao.cedula);
      if (subtotal !== undefined) {
        const centavos = quantidadeDe(entradas.get(definicao.cedula)) * definicao.centavos;
        subtotal.textContent = centavos === 0 ? "—" : formatarMoeda(centavos / CENTAVOS_POR_REAL);
      }
    }
    const total = centavosTotais() / CENTAVOS_POR_REAL;
    valorTotal.textContent = formatarMoeda(total);
    aoMudar?.(total);
  };

  const criarGrupo = (titulo: string, definicoes: readonly DefinicaoCedula[]): HTMLElement => {
    const cabecalho = document.createElement("h3");
    cabecalho.className = "contagem-cedulas__titulo";
    cabecalho.textContent = titulo;
    const linhas = definicoes.map((definicao) => {
      const id = `cedula-${definicao.cedula}-${proximoIdContagem()}`;
      const rotulo = document.createElement("label");
      rotulo.htmlFor = id;
      rotulo.className = "contagem-cedulas__rotulo";
      rotulo.textContent = definicao.rotulo;

      const entrada = document.createElement("input");
      entrada.id = id;
      entrada.type = "number";
      entrada.min = "0";
      entrada.max = String(QUANTIDADE_MAXIMA);
      entrada.step = "1";
      entrada.inputMode = "numeric";
      entrada.placeholder = "0";
      entrada.className = "contagem-cedulas__quantidade";
      const quantidadeInicial = inicial[definicao.cedula] ?? 0;
      entrada.value = quantidadeInicial > 0 ? String(quantidadeInicial) : "";
      entrada.addEventListener("input", atualizar);
      entradas.set(definicao.cedula, entrada);

      const subtotal = document.createElement("span");
      subtotal.className = "contagem-cedulas__subtotal";
      subtotais.set(definicao.cedula, subtotal);

      const linha = document.createElement("div");
      linha.className = "contagem-cedulas__linha";
      linha.append(rotulo, entrada, subtotal);
      return linha;
    });
    const grupo = document.createElement("div");
    grupo.className = "contagem-cedulas__grupo";
    grupo.append(cabecalho, ...linhas);
    return grupo;
  };

  const grupos = document.createElement("div");
  grupos.className = "contagem-cedulas__grupos";
  grupos.append(criarGrupo("Notas", NOTAS), criarGrupo("Moedas", MOEDAS));

  const rotuloTotal = document.createElement("span");
  rotuloTotal.textContent = "Total contado";
  const rodape = document.createElement("div");
  rodape.className = "contagem-cedulas__total";
  rodape.append(rotuloTotal, valorTotal);

  const elemento = document.createElement("div");
  elemento.className = "contagem-cedulas";
  elemento.append(grupos, rodape);
  atualizar();

  return {
    elemento,
    contagem: () => {
      const contagem: Contagem = {};
      entradas.forEach((entrada, cedula) => {
        const quantidade = quantidadeDe(entrada);
        if (quantidade > 0) {
          contagem[cedula] = quantidade;
        }
      });
      return contagem;
    },
    total: () => centavosTotais() / CENTAVOS_POR_REAL,
    focar: () => entradas.get("NOTA_200")?.focus(),
  };
}

/** Campo vazio, negativo ou com vírgula conta como zero (o servidor recusaria de qualquer forma). */
function quantidadeDe(entrada: HTMLInputElement | undefined): number {
  if (entrada === undefined) {
    return 0;
  }
  const quantidade = Number(entrada.value);
  return Number.isInteger(quantidade) && quantidade > 0 ? Math.min(quantidade, QUANTIDADE_MAXIMA) : 0;
}

/** A mesma tela pode ter duas contagens (ex.: fundo padrão e abertura): ids de campo não podem repetir. */
let contagensCriadas = 0;
function proximoIdContagem(): number {
  contagensCriadas += 1;
  return contagensCriadas;
}

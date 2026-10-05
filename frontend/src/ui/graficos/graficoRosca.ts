const SVG_NS = "http://www.w3.org/2000/svg";

/** Especificações fixas da rosca (ver DECISOES D21): anel fino, 2px de folga entre fatias, total no centro. */
const TAMANHO = 176;
const RAIO_EXTERNO = 84;
const RAIO_INTERNO = 58;
const VOLTA_COMPLETA = Math.PI * 2;
/** Começa às 12h e segue no sentido horário. */
const INICIO = -Math.PI / 2;
const MAXIMO_FATIAS = 6;
/** "restante" (cinza) sempre fecha o anel, depois de qualquer slot. */
const POSICAO_DO_RESTANTE = 99;

/**
 * Cor da fatia = slot da paleta categórica (validada com scripts/validate_palette.js, inclusive o
 * par "última ↔ primeira" do anel). A cor segue a categoria, nunca a posição no ranking.
 * "restante" = o que sobra e não é categoria (a receber, sem custo): cinza claro, sempre por último.
 */
export type CorFatia = 1 | 2 | 3 | 4 | 5 | 6 | 7 | "restante";

export interface Fatia {
  rotulo: string;
  valor: number;
  cor: CorFatia;
  /** Texto pequeno na legenda (ex.: "12 vendas"). */
  detalhe: string;
}

export interface OpcoesGraficoRosca {
  fatias: readonly Fatia[];
  formatarValor: (valor: number) => string;
  /** Total escrito no centro (formato curto, ex.: "R$ 1,2 mil"). */
  formatarTotal: (valor: number) => string;
  rotuloTotal: string;
  /** Leitura para leitor de tela. */
  descricao: string;
}

const PERCENTUAL = new Intl.NumberFormat("pt-BR", { style: "percent", maximumFractionDigits: 0 });

/**
 * Pizza em formato de rosca, só para "partes de um todo" com até 6 categorias. A legenda ao lado
 * traz rótulo, valor e porcentagem de cada fatia — a cor nunca é a única forma de ler o número.
 * Passar o mouse (ou o foco) numa fatia ou linha da legenda destaca as duas e mostra a fatia no centro.
 */
export function criarGraficoRosca(opcoes: OpcoesGraficoRosca): HTMLElement {
  const fatias = ordenarPorCor(opcoes.fatias.filter((fatia) => fatia.valor > 0));
  if (fatias.length > MAXIMO_FATIAS + 1) {
    throw new Error(`Rosca com ${fatias.length} fatias: use barras para mais de ${MAXIMO_FATIAS} categorias.`);
  }
  const total = fatias.reduce((soma, fatia) => soma + fatia.valor, 0);

  const valorCentro = document.createElement("strong");
  valorCentro.className = "rosca__centro-valor";
  const rotuloCentro = document.createElement("span");
  rotuloCentro.className = "rosca__centro-rotulo";
  const mostrarTotal = (): void => {
    valorCentro.textContent = opcoes.formatarTotal(total);
    rotuloCentro.textContent = opcoes.rotuloTotal;
  };
  mostrarTotal();
  const centro = document.createElement("div");
  centro.className = "rosca__centro";
  centro.setAttribute("aria-hidden", "true");
  centro.append(valorCentro, rotuloCentro);

  const svg = elementoSvg("svg");
  svg.setAttribute("width", String(TAMANHO));
  svg.setAttribute("height", String(TAMANHO));
  svg.setAttribute("viewBox", `0 0 ${TAMANHO} ${TAMANHO}`);
  svg.setAttribute("role", "img");
  svg.setAttribute("aria-label", opcoes.descricao);

  const legenda = document.createElement("ul");
  legenda.className = "rosca__legenda";

  let angulo = INICIO;
  fatias.forEach((fatia) => {
    const fracao = fatia.valor / total;
    const marca = elementoSvg("path");
    marca.setAttribute("d", caminhoFatia(angulo, angulo + fracao * VOLTA_COMPLETA));
    marca.setAttribute("class", `rosca__fatia rosca__fatia--${fatia.cor}`);
    angulo += fracao * VOLTA_COMPLETA;

    const item = criarItemLegenda(fatia, fracao, opcoes.formatarValor);
    const destacar = (): void => {
      svg.classList.add("rosca--com-destaque");
      marca.classList.add("rosca__fatia--ativa");
      item.classList.add("rosca__item--ativo");
      valorCentro.textContent = PERCENTUAL.format(fracao);
      rotuloCentro.textContent = fatia.rotulo;
    };
    const apagar = (): void => {
      svg.classList.remove("rosca--com-destaque");
      marca.classList.remove("rosca__fatia--ativa");
      item.classList.remove("rosca__item--ativo");
      mostrarTotal();
    };
    for (const alvo of [marca, item]) {
      alvo.addEventListener("pointerenter", destacar);
      alvo.addEventListener("pointerleave", apagar);
    }
    item.addEventListener("focus", destacar);
    item.addEventListener("blur", apagar);

    svg.append(marca);
    legenda.append(item);
  });

  const desenho = document.createElement("div");
  desenho.className = "rosca__desenho";
  desenho.append(svg, centro);

  const container = document.createElement("div");
  container.className = "rosca";
  container.append(desenho, legenda);
  return container;
}

function criarItemLegenda(fatia: Fatia, fracao: number, formatarValor: (valor: number) => string): HTMLLIElement {
  const amostra = document.createElement("span");
  amostra.className = `rosca__amostra rosca__fatia--${fatia.cor}`;
  const rotulo = document.createElement("span");
  rotulo.className = "rosca__rotulo";
  rotulo.textContent = fatia.rotulo;
  const percentual = document.createElement("span");
  percentual.className = "rosca__percentual";
  percentual.textContent = PERCENTUAL.format(fracao);
  const valor = document.createElement("strong");
  valor.className = "rosca__valor";
  valor.textContent = formatarValor(fatia.valor);
  const detalhe = document.createElement("span");
  detalhe.className = "rosca__detalhe";
  detalhe.textContent = fatia.detalhe;

  const item = document.createElement("li");
  item.className = "rosca__item";
  item.tabIndex = 0;
  item.setAttribute("aria-label", `${fatia.rotulo}: ${formatarValor(fatia.valor)}, ${PERCENTUAL.format(fracao)}`);
  item.append(amostra, rotulo, percentual, valor, detalhe);
  return item;
}

/** A ordem do anel é a ordem da paleta (validada par a par); "restante" fecha a volta. */
function ordenarPorCor(fatias: readonly Fatia[]): Fatia[] {
  const posicao = (cor: CorFatia): number => (cor === "restante" ? POSICAO_DO_RESTANTE : cor);
  return [...fatias].sort((a, b) => posicao(a.cor) - posicao(b.cor));
}

/** Setor do anel entre dois ângulos. Uma fatia de 100% vira duas metades (o arco SVG não fecha sozinho). */
function caminhoFatia(inicio: number, fim: number): string {
  if (fim - inicio >= VOLTA_COMPLETA - 1e-6) {
    const meio = inicio + Math.PI;
    return `${caminhoFatia(inicio, meio)} ${caminhoFatia(meio, fim)}`;
  }
  const grande = fim - inicio > Math.PI ? 1 : 0;
  const [x1, y1] = ponto(RAIO_EXTERNO, inicio);
  const [x2, y2] = ponto(RAIO_EXTERNO, fim);
  const [x3, y3] = ponto(RAIO_INTERNO, fim);
  const [x4, y4] = ponto(RAIO_INTERNO, inicio);
  return [
    `M${x1},${y1}`,
    `A${RAIO_EXTERNO},${RAIO_EXTERNO} 0 ${grande} 1 ${x2},${y2}`,
    `L${x3},${y3}`,
    `A${RAIO_INTERNO},${RAIO_INTERNO} 0 ${grande} 0 ${x4},${y4}`,
    "Z",
  ].join(" ");
}

function ponto(raio: number, angulo: number): readonly [number, number] {
  const meio = TAMANHO / 2;
  return [arredondar(meio + raio * Math.cos(angulo)), arredondar(meio + raio * Math.sin(angulo))];
}

function arredondar(valor: number): number {
  return Math.round(valor * 100) / 100;
}

function elementoSvg<K extends keyof SVGElementTagNameMap>(tag: K): SVGElementTagNameMap[K] {
  return document.createElementNS(SVG_NS, tag) as SVGElementTagNameMap[K];
}

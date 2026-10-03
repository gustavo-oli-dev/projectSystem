const SVG_NS = "http://www.w3.org/2000/svg";

/** Especificações fixas dos gráficos (ver DECISOES D21): coluna fina, topo arredondado, grade fina. */
const LARGURA_MAXIMA_COLUNA = 24;
const RAIO_TOPO = 4;
const LINHAS_DE_GRADE = 4;
const MARGEM = { topo: 12, direita: 8, base: 30, esquerda: 72 };
const LARGURA_ESTIMADA_ROTULO = 52;
const LARGURA_PADRAO = 800;
const PASSOS_REDONDOS = [1, 2, 2.5, 5, 10];

export interface Coluna {
  /** Texto curto embaixo da coluna (ex.: "12/09"). */
  rotulo: string;
  /** Título da dica (ex.: "sexta, 12 de setembro"). */
  rotuloCompleto: string;
  valor: number;
  /** Linhas extras da dica: [nome, valor]. */
  detalhes: ReadonlyArray<readonly [string, string]>;
}

export interface OpcoesGraficoColunas {
  colunas: readonly Coluna[];
  formatarValor: (valor: number) => string;
  formatarEixo: (valor: number) => string;
  /** Leitura para leitor de tela (ex.: "Faturamento por dia em setembro"). */
  descricao: string;
  alturaPlot?: number;
}

/**
 * Gráfico de colunas de uma série (uma cor só). Interativo: cada coluna é o alvo da dica, no
 * mouse e no teclado. Redesenha quando o espaço muda de largura.
 */
export function criarGraficoColunas(opcoes: OpcoesGraficoColunas): HTMLElement {
  const container = document.createElement("div");
  container.className = "grafico";

  const dica = document.createElement("div");
  dica.className = "grafico__dica";
  dica.hidden = true;
  dica.setAttribute("role", "status");

  let larguraDesenhada = 0;
  const desenhar = (): void => {
    const largura = Math.round(container.clientWidth) || LARGURA_PADRAO;
    if (largura === larguraDesenhada) {
      return;
    }
    larguraDesenhada = largura;
    container.replaceChildren(montarSvg(opcoes, largura, container, dica), dica);
  };

  new ResizeObserver(desenhar).observe(container);
  queueMicrotask(desenhar);
  return container;
}

function montarSvg(
  opcoes: OpcoesGraficoColunas, largura: number, container: HTMLElement, dica: HTMLElement
): SVGSVGElement {
  const alturaPlot = opcoes.alturaPlot ?? 240;
  const altura = alturaPlot + MARGEM.topo + MARGEM.base;
  const larguraPlot = Math.max(largura - MARGEM.esquerda - MARGEM.direita, 1);
  const maximo = maximoRedondo(Math.max(0, ...opcoes.colunas.map((coluna) => coluna.valor)));

  const svg = elementoSvg("svg");
  svg.setAttribute("width", String(largura));
  svg.setAttribute("height", String(altura));
  svg.setAttribute("viewBox", `0 0 ${largura} ${altura}`);
  svg.setAttribute("role", "img");
  svg.setAttribute("aria-label", opcoes.descricao);

  const y = (valor: number): number => MARGEM.topo + alturaPlot - (valor / maximo) * alturaPlot;

  for (let passo = 0; passo <= LINHAS_DE_GRADE; passo++) {
    const valor = (maximo / LINHAS_DE_GRADE) * passo;
    const linha = elementoSvg("line");
    linha.setAttribute("x1", String(MARGEM.esquerda));
    linha.setAttribute("x2", String(largura - MARGEM.direita));
    linha.setAttribute("y1", String(y(valor)));
    linha.setAttribute("y2", String(y(valor)));
    linha.setAttribute("class", passo === 0 ? "grafico__base" : "grafico__grade");
    svg.append(linha, texto(opcoes.formatarEixo(valor), MARGEM.esquerda - 10, y(valor) + 4, "grafico__eixo grafico__eixo--y"));
  }

  const faixa = larguraPlot / Math.max(opcoes.colunas.length, 1);
  const larguraColuna = Math.min(LARGURA_MAXIMA_COLUNA, faixa * 0.6);
  const intervaloRotulos = Math.max(1, Math.ceil((opcoes.colunas.length * LARGURA_ESTIMADA_ROTULO) / larguraPlot));

  opcoes.colunas.forEach((coluna, indice) => {
    const centro = MARGEM.esquerda + faixa * indice + faixa / 2;
    const marca = elementoSvg("path");
    marca.setAttribute("d", caminhoColuna(centro - larguraColuna / 2, larguraColuna, y(coluna.valor), y(0)));
    marca.setAttribute("class", "grafico__coluna");

    // Alvo da dica: a faixa inteira (bem maior que a coluna), no mouse e no teclado.
    const alvo = elementoSvg("rect");
    alvo.setAttribute("x", String(centro - faixa / 2));
    alvo.setAttribute("y", String(MARGEM.topo));
    alvo.setAttribute("width", String(faixa));
    alvo.setAttribute("height", String(alturaPlot));
    alvo.setAttribute("class", "grafico__alvo");
    alvo.setAttribute("tabindex", "0");
    alvo.setAttribute("aria-label", `${coluna.rotuloCompleto}: ${opcoes.formatarValor(coluna.valor)}`);
    const mostrar = (): void => {
      marca.classList.add("grafico__coluna--ativa");
      preencherDica(dica, coluna, opcoes.formatarValor);
      posicionarDica(dica, container, centro, y(coluna.valor));
    };
    const esconder = (): void => {
      marca.classList.remove("grafico__coluna--ativa");
      dica.hidden = true;
    };
    alvo.addEventListener("pointerenter", mostrar);
    alvo.addEventListener("focus", mostrar);
    alvo.addEventListener("pointerleave", esconder);
    alvo.addEventListener("blur", esconder);

    svg.append(marca, alvo);
    if (indice % intervaloRotulos === 0) {
      svg.append(texto(coluna.rotulo, centro, altura - 8, "grafico__eixo grafico__eixo--x"));
    }
  });
  return svg;
}

/** Retângulo com os dois cantos de cima arredondados e a base reta, na linha do zero. */
function caminhoColuna(x: number, largura: number, topo: number, base: number): string {
  const altura = base - topo;
  if (altura <= 0) {
    return "";
  }
  const raio = Math.min(RAIO_TOPO, altura, largura / 2);
  return [
    `M${x},${base}`,
    `V${topo + raio}`,
    `Q${x},${topo} ${x + raio},${topo}`,
    `H${x + largura - raio}`,
    `Q${x + largura},${topo} ${x + largura},${topo + raio}`,
    `V${base}`,
    "Z",
  ].join(" ");
}

/** Topo do eixo num número redondo, com 4 divisões limpas (0 / 500 / 1.000 ...). */
function maximoRedondo(maior: number): number {
  if (maior <= 0) {
    return LINHAS_DE_GRADE;
  }
  const bruto = maior / LINHAS_DE_GRADE;
  const magnitude = 10 ** Math.floor(Math.log10(bruto));
  const passo = (PASSOS_REDONDOS.find((candidato) => candidato * magnitude >= bruto) ?? 10) * magnitude;
  return passo * LINHAS_DE_GRADE;
}

function preencherDica(dica: HTMLElement, coluna: Coluna, formatarValor: (valor: number) => string): void {
  const valor = document.createElement("strong");
  valor.textContent = formatarValor(coluna.valor);
  const titulo = document.createElement("span");
  titulo.className = "grafico__dica-titulo";
  titulo.textContent = coluna.rotuloCompleto;
  const linhas = coluna.detalhes.map(([nome, conteudo]) => {
    const linha = document.createElement("span");
    linha.className = "grafico__dica-linha";
    linha.textContent = `${nome}: ${conteudo}`;
    return linha;
  });
  dica.replaceChildren(valor, titulo, ...linhas);
  dica.hidden = false;
}

function posicionarDica(dica: HTMLElement, container: HTMLElement, centro: number, topoColuna: number): void {
  const larguraDica = dica.offsetWidth;
  const esquerda = Math.min(Math.max(centro - larguraDica / 2, 0), container.clientWidth - larguraDica);
  dica.style.left = `${esquerda}px`;
  dica.style.top = `${Math.max(topoColuna - dica.offsetHeight - 10, 0)}px`;
}

function texto(conteudo: string, x: number, y: number, classe: string): SVGTextElement {
  const elemento = elementoSvg("text");
  elemento.setAttribute("x", String(x));
  elemento.setAttribute("y", String(y));
  elemento.setAttribute("class", classe);
  elemento.textContent = conteudo;
  return elemento;
}

function elementoSvg<K extends keyof SVGElementTagNameMap>(tag: K): SVGElementTagNameMap[K] {
  return document.createElementNS(SVG_NS, tag) as SVGElementTagNameMap[K];
}

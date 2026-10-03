const SVG_NS = "http://www.w3.org/2000/svg";

function elementoSvg<K extends keyof SVGElementTagNameMap>(tag: K): SVGElementTagNameMap[K] {
  return document.createElementNS(SVG_NS, tag) as SVGElementTagNameMap[K];
}

function retangulo(x: number, y: number, largura: number, altura: number, raio = 0): SVGRectElement {
  const forma = elementoSvg("rect");
  forma.setAttribute("x", String(x));
  forma.setAttribute("y", String(y));
  forma.setAttribute("width", String(largura));
  forma.setAttribute("height", String(altura));
  if (raio > 0) {
    forma.setAttribute("rx", String(raio));
  }
  return forma;
}

function circulo(cx: number, cy: number, raio: number): SVGCircleElement {
  const forma = elementoSvg("circle");
  forma.setAttribute("cx", String(cx));
  forma.setAttribute("cy", String(cy));
  forma.setAttribute("r", String(raio));
  return forma;
}

function linha(x1: number, y1: number, x2: number, y2: number): SVGLineElement {
  const forma = elementoSvg("line");
  forma.setAttribute("x1", String(x1));
  forma.setAttribute("y1", String(y1));
  forma.setAttribute("x2", String(x2));
  forma.setAttribute("y2", String(y2));
  return forma;
}

function formasPara(nome: string): SVGElement[] {
  switch (nome) {
    case "painel":
      return [
        retangulo(3, 3, 7, 7, 1.5),
        retangulo(14, 3, 7, 7, 1.5),
        retangulo(3, 14, 7, 7, 1.5),
        retangulo(14, 14, 7, 7, 1.5),
      ];
    case "pedidos":
      return [linha(4, 6, 20, 6), linha(4, 12, 20, 12), linha(4, 18, 14, 18)];
    case "clientes":
      return [circulo(12, 8, 4), retangulo(5, 15, 14, 7, 3.5)];
    case "produtos":
      return [retangulo(3, 7, 18, 14, 2), linha(3, 7, 12, 3), linha(12, 3, 21, 7)];
    case "servicos":
      return [circulo(12, 12, 8), circulo(12, 12, 3)];
    case "novo":
      return [circulo(12, 12, 9), linha(12, 8, 12, 16), linha(8, 12, 16, 12)];
    case "fiscal":
      return [retangulo(5, 3, 14, 18, 2), linha(8, 8, 16, 8), linha(8, 12, 16, 12), linha(8, 16, 12, 16)];
    case "cobrancas":
      return [retangulo(3, 6, 18, 12, 2), linha(3, 10, 21, 10), linha(7, 14, 11, 14)];
    case "atendimento":
      return [retangulo(3, 4, 18, 13, 3), linha(8, 21, 12, 17)];
    case "busca":
      return [circulo(11, 11, 6), linha(16, 16, 20, 20)];
    case "mais":
      return [linha(12, 6, 12, 18), linha(6, 12, 18, 12)];
    case "equipe":
      return [circulo(9, 8, 3.5), circulo(17, 9, 2.5), retangulo(3, 14, 12, 7, 3.5), linha(17, 14, 21, 18)];
    case "assistente":
      return [retangulo(3, 4, 18, 13, 3), circulo(9, 10.5, 1), circulo(15, 10.5, 1), linha(12, 17, 12, 21)];
    case "gerenciar":
      return [retangulo(3, 7, 14, 14, 2), linha(3, 7, 10, 3), linha(10, 3, 17, 7), linha(19, 11, 19, 19), linha(15, 15, 23, 15)];
    case "caixa":
      return [retangulo(3, 10, 18, 11, 2), retangulo(6, 3, 12, 7, 1.5), linha(7, 14, 9, 14), linha(11, 14, 13, 14), linha(15, 14, 17, 14), linha(7, 17, 17, 17)];
    case "fechar":
      return [linha(6, 6, 18, 18), linha(18, 6, 6, 18)];
    default:
      return [circulo(12, 12, 9)];
  }
}

export function criarIcone(nome: string): SVGSVGElement {
  const svg = elementoSvg("svg");
  svg.setAttribute("viewBox", "0 0 24 24");
  svg.setAttribute("width", "18");
  svg.setAttribute("height", "18");
  svg.setAttribute("fill", "none");
  svg.setAttribute("stroke", "currentColor");
  svg.setAttribute("stroke-width", "1.8");
  svg.setAttribute("stroke-linecap", "round");
  svg.setAttribute("stroke-linejoin", "round");
  svg.classList.add("icone-nav");
  svg.append(...formasPara(nome));
  return svg;
}

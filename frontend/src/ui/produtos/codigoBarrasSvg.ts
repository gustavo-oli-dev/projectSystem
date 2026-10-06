/**
 * Desenha o código de barras EAN-13 (e UPC-A, que é um EAN-13 com 0 na frente) em SVG, para a
 * etiqueta de gôndola. Outros formatos (EAN-8, GTIN-14) ficam só com os números.
 */
const SVG = "http://www.w3.org/2000/svg";
const MODULOS = 95;
const ALTURA = 40;
const DIGITOS_EAN13 = 13;
const DIGITOS_UPC = 12;

const CODIGO_L = ["0001101", "0011001", "0010011", "0111101", "0100011", "0110001", "0101111", "0111011", "0110111", "0001011"];
const CODIGO_G = ["0100111", "0110011", "0011011", "0100001", "0011101", "0111001", "0000101", "0010001", "0001001", "0010111"];
const CODIGO_R = ["1110010", "1100110", "1101100", "1000010", "1011100", "1001110", "1010000", "1000100", "1001000", "1110100"];
/** O primeiro dígito não vira barra: ele decide se cada dígito da esquerda usa o código L ou G. */
const PARIDADE = ["LLLLLL", "LLGLGG", "LLGGLG", "LLGGGL", "LGLLGG", "LGGLLG", "LGGGLL", "LGLGLG", "LGLGGL", "LGGLGL"];
const GUARDA_LATERAL = "101";
const GUARDA_CENTRAL = "01010";

export function criarCodigoBarras(codigo: string): SVGSVGElement | null {
  const ean = codigo.length === DIGITOS_UPC ? `0${codigo}` : codigo;
  if (!/^\d+$/.test(ean) || ean.length !== DIGITOS_EAN13) {
    return null;
  }
  const digitos = [...ean].map(Number);
  const paridade = PARIDADE[digitos[0] ?? 0] ?? PARIDADE[0] ?? "";
  const esquerda = digitos.slice(1, 7).map((digito, posicao) =>
    (paridade[posicao] === "G" ? CODIGO_G : CODIGO_L)[digito] ?? "").join("");
  const direita = digitos.slice(7).map((digito) => CODIGO_R[digito] ?? "").join("");
  const bits = GUARDA_LATERAL + esquerda + GUARDA_CENTRAL + direita + GUARDA_LATERAL;

  const svg = document.createElementNS(SVG, "svg");
  svg.setAttribute("viewBox", `0 0 ${MODULOS} ${ALTURA}`);
  svg.setAttribute("preserveAspectRatio", "none");
  svg.setAttribute("class", "etiqueta__barras");
  svg.setAttribute("role", "img");
  svg.setAttribute("aria-label", `Código de barras ${codigo}`);
  let inicio = -1;
  for (let indice = 0; indice <= bits.length; indice++) {
    const preta = bits[indice] === "1";
    if (preta && inicio < 0) {
      inicio = indice;
    } else if (!preta && inicio >= 0) {
      const barra = document.createElementNS(SVG, "rect");
      barra.setAttribute("x", String(inicio));
      barra.setAttribute("y", "0");
      barra.setAttribute("width", String(indice - inicio));
      barra.setAttribute("height", String(ALTURA));
      svg.append(barra);
      inicio = -1;
    }
  }
  return svg;
}

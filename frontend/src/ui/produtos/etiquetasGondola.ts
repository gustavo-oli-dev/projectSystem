import type { Produto } from "../../api/produtosApi.js";
import type { Promocao } from "../../api/promocoesApi.js";
import { formatarMoeda } from "../formatarMoeda.js";
import { criarCodigoBarras } from "./codigoBarrasSvg.js";
import { descreverPromocao } from "./descricaoPromocao.js";

const DATA = new Intl.DateTimeFormat("pt-BR", { dateStyle: "short" });

/**
 * Etiquetas de gôndola (D39): monta uma folha só para impressão (o resto da tela some no
 * @media print) e chama a impressão do navegador. Preço de oferta do dia sai em destaque.
 */
export function imprimirEtiquetas(produtos: readonly Produto[], promocoes: readonly Promocao[]): void {
  if (produtos.length === 0) {
    return;
  }
  const porProduto = new Map(promocoes.map((promocao) => [promocao.produtoId, promocao]));
  const folha = document.createElement("div");
  folha.className = "etiquetas-impressao";
  folha.append(...produtos.map((produto) => criarEtiqueta(produto, porProduto.get(produto.id) ?? null)));
  document.body.append(folha);
  const remover = (): void => {
    folha.remove();
    window.removeEventListener("afterprint", remover);
  };
  window.addEventListener("afterprint", remover);
  window.print();
}

function criarEtiqueta(produto: Produto, promocao: Promocao | null): HTMLElement {
  const nome = document.createElement("p");
  nome.className = "etiqueta__nome";
  nome.textContent = produto.nome;

  const preco = document.createElement("p");
  preco.className = "etiqueta__preco";
  const oferta = promocao !== null && promocao.tipo === "PRECO_OFERTA" && promocao.precoOferta !== null
    && promocao.precoOferta < produto.precoUnitario ? promocao.precoOferta : null;
  preco.textContent = formatarMoeda(oferta ?? produto.precoUnitario);
  const unidade = document.createElement("span");
  unidade.className = "etiqueta__unidade";
  unidade.textContent = ` / ${produto.unidadeMedida}`;
  preco.append(unidade);

  const etiqueta = document.createElement("div");
  etiqueta.className = "etiqueta";
  etiqueta.append(nome);
  if (promocao !== null) {
    const destaque = document.createElement("p");
    destaque.className = "etiqueta__promocao";
    destaque.textContent = oferta === null
      ? descreverPromocao(promocao)
      : `Oferta · de ${formatarMoeda(produto.precoUnitario)} por`;
    etiqueta.append(destaque);
  }
  etiqueta.append(preco);

  if (produto.codigoBarras !== null) {
    const barras = criarCodigoBarras(produto.codigoBarras);
    if (barras !== null) {
      etiqueta.append(barras);
    }
    const numeros = document.createElement("p");
    numeros.className = "etiqueta__codigo";
    numeros.textContent = produto.codigoBarras;
    etiqueta.append(numeros);
  }
  const data = document.createElement("p");
  data.className = "etiqueta__data";
  data.textContent = `Preço de ${DATA.format(new Date())}`;
  etiqueta.append(data);
  return etiqueta;
}

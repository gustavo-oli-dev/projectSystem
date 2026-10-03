import type { Produto } from "../api/produtosApi.js";

/** Carrinho da venda em andamento no caixa. Vive só na tela; nada vai ao servidor até finalizar. */
export interface ItemCarrinho {
  readonly produto: Produto;
  quantidade: number;
}

type Ouvinte = () => void;

let itens: ItemCarrinho[] = [];
const ouvintes: Ouvinte[] = [];

export function itensDoCarrinho(): readonly ItemCarrinho[] {
  return itens;
}

/** Ler o mesmo produto de novo soma uma unidade em vez de criar outra linha. */
export function adicionarAoCarrinho(produto: Produto): void {
  const existente = itens.find((item) => item.produto.id === produto.id);
  if (existente === undefined) {
    itens = [...itens, { produto, quantidade: 1 }];
  } else {
    existente.quantidade += 1;
  }
  notificar();
}

export function alterarQuantidade(produtoId: string, quantidade: number): void {
  itens = quantidade <= 0
    ? itens.filter((item) => item.produto.id !== produtoId)
    : itens.map((item) => (item.produto.id === produtoId ? { ...item, quantidade } : item));
  notificar();
}

export function limparCarrinho(): void {
  itens = [];
  notificar();
}

export function totalDoCarrinho(): number {
  return itens.reduce((soma, item) => soma + item.produto.precoUnitario * item.quantidade, 0);
}

/** Substitui o ouvinte anterior: só existe uma tela de caixa aberta por vez. */
export function aoMudarCarrinho(ouvinte: Ouvinte): void {
  ouvintes.length = 0;
  ouvintes.push(ouvinte);
}

function notificar(): void {
  ouvintes.forEach((ouvinte) => ouvinte());
}

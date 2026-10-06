import type { Produto } from "../api/produtosApi.js";
import { descontoDaPromocao } from "./promocoesDoDia.js";

/** Carrinho da venda em andamento no caixa. Vive só na tela; nada vai ao servidor até finalizar. */
export interface ItemCarrinho {
  readonly produto: Produto;
  quantidade: number;
}

/** Desconto autorizado por um gerente para ESTA venda (D35). */
export interface DescontoNaVenda {
  readonly valor: number;
  readonly tokenAutorizacao: string;
  readonly autorizadoPorNome: string;
}

type Ouvinte = () => void;

let itens: ItemCarrinho[] = [];
let desconto: DescontoNaVenda | null = null;
const ouvintes: Ouvinte[] = [];

export function itensDoCarrinho(): readonly ItemCarrinho[] {
  return itens;
}

/** Ler o mesmo produto de novo soma uma unidade em vez de criar outra linha. */
export function adicionarAoCarrinho(produto: Produto): void {
  desconto = null;
  const existente = itens.find((item) => item.produto.id === produto.id);
  if (existente === undefined) {
    itens = [...itens, { produto, quantidade: 1 }];
  } else {
    existente.quantidade += 1;
  }
  notificar();
}

export function alterarQuantidade(produtoId: string, quantidade: number): void {
  const mudou = itens.some((item) => item.produto.id === produtoId && item.quantidade !== quantidade);
  if (mudou) {
    desconto = null;
  }
  itens = quantidade <= 0
    ? itens.filter((item) => item.produto.id !== produtoId)
    : itens.map((item) => (item.produto.id === produtoId ? { ...item, quantidade } : item));
  notificar();
}

export function limparCarrinho(): void {
  itens = [];
  desconto = null;
  notificar();
}

/** Desconto da promoção do dia nesta linha do carrinho (zero sem promoção). */
export function descontoPromocaoDoItem(item: ItemCarrinho): number {
  return descontoDaPromocao(item.produto.id, item.produto.precoUnitario, item.quantidade);
}

export function subtotalDoItem(item: ItemCarrinho): number {
  return item.produto.precoUnitario * item.quantidade - descontoPromocaoDoItem(item);
}

/** Soma dos itens já com as promoções do dia (antes do desconto do gerente). */
export function totalDoCarrinho(): number {
  return itens.reduce((soma, item) => soma + subtotalDoItem(item), 0);
}

/**
 * O desconto vale para a venda como ela estava quando o gerente autorizou: qualquer mudança no
 * carrinho o retira (é preciso pedir de novo).
 */
export function definirDesconto(novo: DescontoNaVenda | null): void {
  desconto = novo;
  notificar();
}

export function descontoDaVenda(): DescontoNaVenda | null {
  return desconto;
}

/** O que o cliente paga: soma dos itens − desconto. */
export function totalAPagar(): number {
  return Math.max(0, totalDoCarrinho() - (desconto?.valor ?? 0));
}

/** Substitui o ouvinte anterior: só existe uma tela de caixa aberta por vez. */
export function aoMudarCarrinho(ouvinte: Ouvinte): void {
  ouvintes.length = 0;
  ouvintes.push(ouvinte);
}

function notificar(): void {
  ouvintes.forEach((ouvinte) => ouvinte());
}

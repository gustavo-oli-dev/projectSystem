import type { Embalagem, Produto } from "../api/produtosApi.js";
import { descontoDaPromocao } from "./promocoesDoDia.js";

/** Carrinho da venda em andamento no caixa. Vive só na tela; nada vai ao servidor até finalizar. */
export interface ItemCarrinho {
  readonly produto: Produto;
  /** Vendido numa embalagem (ex.: fardo com 12) — D41; null = unidade avulsa. */
  readonly embalagem: Embalagem | null;
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

/** A unidade avulsa e cada embalagem do mesmo produto são linhas diferentes do carrinho. */
export function chaveDoItem(item: Pick<ItemCarrinho, "produto" | "embalagem">): string {
  return `${item.produto.id}:${item.embalagem?.id ?? ""}`;
}

/** Ler o mesmo produto (ou a mesma embalagem) de novo soma uma em vez de criar outra linha. */
export function adicionarAoCarrinho(produto: Produto, embalagem: Embalagem | null = null): void {
  desconto = null;
  const chave = chaveDoItem({ produto, embalagem });
  const existente = itens.find((item) => chaveDoItem(item) === chave);
  if (existente === undefined) {
    itens = [...itens, { produto, embalagem, quantidade: 1 }];
  } else {
    existente.quantidade += 1;
  }
  notificar();
}

export function alterarQuantidade(chave: string, quantidade: number): void {
  const mudou = itens.some((item) => chaveDoItem(item) === chave && item.quantidade !== quantidade);
  if (mudou) {
    desconto = null;
  }
  itens = quantidade <= 0
    ? itens.filter((item) => chaveDoItem(item) !== chave)
    : itens.map((item) => (chaveDoItem(item) === chave ? { ...item, quantidade } : item));
  notificar();
}

export function limparCarrinho(): void {
  itens = [];
  desconto = null;
  notificar();
}

/** Preço da linha: o da embalagem, ou o da unidade. */
export function precoDoItem(item: ItemCarrinho): number {
  return item.embalagem?.preco ?? item.produto.precoUnitario;
}

/** Quantas unidades do estoque a linha usa (fardo com 12 × 2 = 24). */
export function unidadesDoItem(item: ItemCarrinho): number {
  return item.quantidade * (item.embalagem?.unidades ?? 1);
}

/** Nome na tela: "Refrigerante — Fardo com 12". */
export function nomeDoItem(item: ItemCarrinho): string {
  return item.embalagem === null ? item.produto.nome : `${item.produto.nome} — ${item.embalagem.nome}`;
}

/** Desconto da promoção do dia nesta linha (zero sem promoção; embalagem tem preço próprio, sem promoção). */
export function descontoPromocaoDoItem(item: ItemCarrinho): number {
  if (item.embalagem !== null) {
    return 0;
  }
  return descontoDaPromocao(item.produto.id, item.produto.precoUnitario, item.quantidade);
}

export function subtotalDoItem(item: ItemCarrinho): number {
  return precoDoItem(item) * item.quantidade - descontoPromocaoDoItem(item);
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

import type { Promocao, TipoPromocao } from "../api/promocoesApi.js";

const CENTAVOS_POR_REAL = 100;

/**
 * Promoções que valem hoje, por produto (D38). O caixa usa para mostrar o preço certo antes de
 * vender; quem vale é o servidor, que recalcula ao fechar a venda com a mesma conta.
 */
let porProduto = new Map<string, Promocao>();

export function definirPromocoesDoDia(promocoes: readonly Promocao[]): void {
  porProduto = new Map(promocoes.map((promocao) => [promocao.produtoId, promocao]));
}

export function promocaoDoProduto(produtoId: string): Promocao | null {
  return porProduto.get(produtoId) ?? null;
}

/** Mesma conta do servidor (TipoPromocao). */
const DESCONTO_POR_TIPO: Record<TipoPromocao, (promocao: Promocao, preco: number, quantidade: number) => number> = {
  PRECO_OFERTA: (promocao, preco, quantidade) =>
    promocao.precoOferta === null || promocao.precoOferta >= preco ? 0 : (preco - promocao.precoOferta) * quantidade,
  LEVE_PAGUE: (promocao, preco, quantidade) => {
    if (promocao.leve === null || promocao.pague === null) {
      return 0;
    }
    return Math.floor(quantidade / promocao.leve) * (promocao.leve - promocao.pague) * preco;
  },
};

/** Desconto da promoção do dia para esta quantidade (zero sem promoção). */
export function descontoDaPromocao(produtoId: string, preco: number, quantidade: number): number {
  const promocao = promocaoDoProduto(produtoId);
  if (promocao === null) {
    return 0;
  }
  return Math.round(DESCONTO_POR_TIPO[promocao.tipo](promocao, preco, quantidade) * CENTAVOS_POR_REAL) / CENTAVOS_POR_REAL;
}

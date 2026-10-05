export type Rota =
  | "painel"
  | "pedidos"
  | "pedido-detalhe"
  | "clientes"
  | "produtos"
  | "servicos"
  | "fiscal"
  | "cobrancas"
  | "conversas"
  | "novo-pedido"
  | "equipe"
  | "gerenciar-produtos"
  | "pdv"
  | "caixas"
  | "gestao-caixa"
  | "inventario"
  | "reposicao"
  | "entrada-nota"
  | "contas-a-pagar"
  | "contatos";

export interface Destino {
  rota: Rota;
  parametro: string | null;
}

const ROTA_PADRAO: Rota = "painel";
const ROTAS_SIMPLES: readonly Rota[] = [
  "painel",
  "pedidos",
  "clientes",
  "produtos",
  "servicos",
  "fiscal",
  "cobrancas",
  "conversas",
  "novo-pedido",
  "equipe",
  "gerenciar-produtos",
  "pdv",
  "caixas",
  "gestao-caixa",
  "inventario",
  "reposicao",
  "entrada-nota",
  "contas-a-pagar",
  "contatos",
];
/** Rotas que aceitam um segundo trecho: aba (equipe, painel) ou produto aberto para edição. */
const ROTAS_COM_PARAMETRO: readonly Rota[] = ["equipe", "gerenciar-produtos", "painel"];

type Ouvinte = (destino: Destino) => void;

/** "#/pedidos" → lista; "#/pedidos/<id>" → detalhe do pedido; "#/equipe/cargos" → aba Cargos. */
export function destinoAtual(): Destino {
  const [primeiro = "", segundo] = window.location.hash.replace(/^#\/?/, "").split("/");
  const temSegundo = segundo !== undefined && segundo !== "";

  if (primeiro === "pedidos" && temSegundo) {
    return { rota: "pedido-detalhe", parametro: segundo };
  }
  if (!ehRotaSimples(primeiro)) {
    return { rota: ROTA_PADRAO, parametro: null };
  }
  return { rota: primeiro, parametro: ROTAS_COM_PARAMETRO.includes(primeiro) && temSegundo ? segundo : null };
}

export function navegarPara(rota: Rota, parametro?: string): void {
  if (rota === "pedido-detalhe" && parametro !== undefined) {
    window.location.hash = `/pedidos/${parametro}`;
    return;
  }
  window.location.hash = parametro !== undefined ? `/${rota}/${parametro}` : `/${rota}`;
}

export function aoMudarRota(ouvinte: Ouvinte): void {
  window.addEventListener("hashchange", () => ouvinte(destinoAtual()));
}

function ehRotaSimples(valor: string): valor is Rota {
  return (ROTAS_SIMPLES as readonly string[]).includes(valor);
}

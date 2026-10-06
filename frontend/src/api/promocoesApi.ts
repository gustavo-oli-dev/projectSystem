import { httpClient } from "./httpClient.js";

export type TipoPromocao = "PRECO_OFERTA" | "LEVE_PAGUE";
export type SituacaoPromocao = "AGENDADA" | "VALENDO" | "TERMINADA" | "ENCERRADA";

export interface Promocao {
  id: string;
  produtoId: string;
  produtoNome: string;
  precoNormal: number;
  tipo: TipoPromocao;
  precoOferta: number | null;
  leve: number | null;
  pague: number | null;
  /** "AAAA-MM-DD", primeiro e último dia (inclusive). */
  inicio: string;
  fim: string;
  situacao: SituacaoPromocao;
}

export interface NovaPromocao {
  produtoId: string;
  tipo: TipoPromocao;
  precoOferta: number | null;
  leve: number | null;
  pague: number | null;
  inicio: string;
  fim: string;
}

export function listarPromocoes(): Promise<Promocao[]> {
  return httpClient.get<Promocao[]>("/promocoes");
}

/** As que valem hoje: o caixa mostra o preço certo antes de fechar (o servidor recalcula ao vender). */
export function listarPromocoesValendoHoje(): Promise<Promocao[]> {
  return httpClient.get<Promocao[]>("/promocoes/valendo-hoje");
}

export function criarPromocao(promocao: NovaPromocao): Promise<Promocao> {
  return httpClient.post<Promocao>("/promocoes", promocao);
}

export function encerrarPromocao(id: string): Promise<Promocao> {
  return httpClient.post<Promocao>(`/promocoes/${id}/encerrar`, undefined);
}

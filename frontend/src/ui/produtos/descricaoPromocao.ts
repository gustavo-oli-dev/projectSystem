import type { Promocao } from "../../api/promocoesApi.js";
import { formatarMoeda } from "../formatarMoeda.js";

/** "Leve 3, pague 2" ou "Oferta R$ 7,90". */
export function descreverPromocao(promocao: Promocao): string {
  if (promocao.tipo === "LEVE_PAGUE") {
    return `Leve ${promocao.leve ?? "?"}, pague ${promocao.pague ?? "?"}`;
  }
  return `Oferta ${formatarMoeda(promocao.precoOferta ?? 0)}`;
}

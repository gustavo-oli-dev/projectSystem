import type { MotivoPerda } from "../../api/produtosApi.js";

export const ROTULO_MOTIVO_PERDA: Record<MotivoPerda, string> = {
  VENCIDO: "Vencido",
  AVARIADO: "Avariado ou quebrado",
  FURTO: "Furto ou extravio",
  USO_INTERNO: "Uso interno",
  OUTRO: "Outro",
};

/** No relatório, a falta achada no inventário aparece como mais um "motivo". */
export function rotuloDoMotivo(chave: string): string {
  if (chave === "INVENTARIO") {
    return "Falta no inventário";
  }
  return chave in ROTULO_MOTIVO_PERDA ? ROTULO_MOTIVO_PERDA[chave as MotivoPerda] : chave;
}

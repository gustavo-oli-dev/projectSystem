/** Abaixo disso o produto aparece como "estoque baixo" — hora de repor. */
const LIMITE_ESTOQUE_BAIXO = 5;

export interface SituacaoEstoque {
  rotulo: string;
  /** Sufixo da classe .selo--* */
  modificador: "esgotado" | "estoque_baixo" | "em_estoque";
}

export function situacaoEstoque(quantidade: number): SituacaoEstoque {
  if (quantidade <= 0) {
    return { rotulo: "Esgotado", modificador: "esgotado" };
  }
  if (quantidade <= LIMITE_ESTOQUE_BAIXO) {
    return { rotulo: "Estoque baixo", modificador: "estoque_baixo" };
  }
  return { rotulo: "Em estoque", modificador: "em_estoque" };
}

/** Sem estoque mínimo definido no produto, vale este aviso (o mesmo padrão do servidor). */
const ESTOQUE_MINIMO_PADRAO = 5;

export interface SituacaoEstoque {
  rotulo: string;
  /** Sufixo da classe .selo--* */
  modificador: "esgotado" | "estoque_baixo" | "em_estoque";
}

/** "Estoque baixo" = chegou no estoque mínimo do produto (ou no padrão, se ele não tiver um). */
export function situacaoEstoque(quantidade: number, estoqueMinimo: number | null = null): SituacaoEstoque {
  if (quantidade <= 0) {
    return { rotulo: "Esgotado", modificador: "esgotado" };
  }
  if (quantidade <= (estoqueMinimo ?? ESTOQUE_MINIMO_PADRAO)) {
    return { rotulo: "Estoque baixo", modificador: "estoque_baixo" };
  }
  return { rotulo: "Em estoque", modificador: "em_estoque" };
}

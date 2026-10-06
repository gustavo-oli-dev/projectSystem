import { httpClient } from "./httpClient.js";
import type { Periodo } from "./relatoriosApi.js";

export interface FluxoDeCaixa {
  totalEntradas: number;
  totalSaidas: number;
  saldo: number;
  /** Contas abertas que vencem no período (ainda não saíram). */
  aPagarNoPeriodo: number;
  /** DINHEIRO, CARTAO_CREDITO, CARTAO_DEBITO, PIX (maquininha), PIX_ONLINE, BOLETO_ONLINE → valor. */
  entradasPorForma: Record<string, number>;
  porDia: Array<{ dia: string; entradas: number; saidas: number; saldo: number }>;
  saidas: Array<{ dia: string; descricao: string; contato: string | null; valor: number }>;
}

function consulta(periodo: Periodo): string {
  return `inicio=${encodeURIComponent(periodo.inicio)}&fim=${encodeURIComponent(periodo.fim)}`;
}

export function gerarFluxoDeCaixa(periodo: Periodo): Promise<FluxoDeCaixa> {
  return httpClient.get<FluxoDeCaixa>(`/financeiro/fluxo-de-caixa?${consulta(periodo)}`);
}

export function baixarLancamentos(periodo: Periodo): Promise<Blob> {
  return httpClient.arquivo(`/financeiro/lancamentos.csv?${consulta(periodo)}`);
}

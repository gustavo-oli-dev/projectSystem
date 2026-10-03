import { httpClient } from "./httpClient.js";

export interface ResultadoFaturamento {
  total: number;
  quantidadeCobrancas: number;
}

export interface ContagemPedidosPorStatus {
  ABERTO: number;
  AGUARDANDO_EMISSAO: number;
  CONCLUIDO: number;
  CANCELADO: number;
}

export interface ResumoCobranca {
  pedidoId: string;
  meio: string;
  valor: number;
  criadoEm: string;
}

export function consultarFaturamento(dataInicio: string, dataFim: string): Promise<ResultadoFaturamento> {
  return httpClient.get<ResultadoFaturamento>(`/painel/faturamento?dataInicio=${dataInicio}&dataFim=${dataFim}`);
}

export function consultarPedidosPorStatus(): Promise<ContagemPedidosPorStatus> {
  return httpClient.get<ContagemPedidosPorStatus>("/painel/pedidos-por-status");
}

export function consultarCobrancasPendentes(): Promise<ResumoCobranca[]> {
  return httpClient.get<ResumoCobranca[]>("/painel/cobrancas-pendentes");
}

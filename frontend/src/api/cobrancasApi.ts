import { httpClient } from "./httpClient.js";

export type MeioCobranca = "PIX" | "BOLETO";

export interface Cobranca {
  id: string;
  pedidoId: string;
  meio: MeioCobranca;
  valor: number;
  status: string;
  qrCodeCopiaECola: string | null;
  linhaDigitavelBoleto: string | null;
  urlBoleto: string | null;
  criadoEm: string;
}

export function criarCobranca(pedidoId: string, meio: MeioCobranca): Promise<Cobranca> {
  return httpClient.post<Cobranca>(`/pedidos/${pedidoId}/cobrancas`, { meio });
}

export function listarCobrancasDoPedido(pedidoId: string): Promise<Cobranca[]> {
  return httpClient.get<Cobranca[]>(`/pedidos/${pedidoId}/cobrancas`);
}

export function listarCobrancas(): Promise<Cobranca[]> {
  return httpClient.get<Cobranca[]>("/cobrancas");
}

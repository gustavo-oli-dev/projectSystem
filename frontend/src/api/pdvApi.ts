import { httpClient } from "./httpClient.js";
import type { ItemPedidoResponse } from "./pedidosApi.js";

export type FormaPagamentoPresencial = "DINHEIRO" | "CARTAO_CREDITO" | "CARTAO_DEBITO" | "PIX";
export type BandeiraCartao = "VISA" | "MASTERCARD" | "ELO" | "AMERICAN_EXPRESS" | "HIPERCARD" | "OUTRA";

export interface PagamentoPresencial {
  forma: FormaPagamentoPresencial;
  valorRecebido: number | null;
  bandeira: BandeiraCartao | null;
  codigoAutorizacao: string | null;
}

export interface NovaVendaBalcao {
  itens: Array<{ produtoId: string; quantidade: number }>;
  cpfNaNota: string | null;
  pagamento: PagamentoPresencial;
}

export interface VendaBalcao {
  pedidoId: string;
  status: string;
  itens: ItemPedidoResponse[];
  total: number;
  cpfNaNota: string | null;
  formaPagamento: FormaPagamentoPresencial;
  bandeira: BandeiraCartao | null;
  codigoAutorizacao: string | null;
  maquininhaIntegrada: boolean;
  valorRecebido: number | null;
  troco: number | null;
  statusPagamento: "APROVADO" | "ESTORNADO";
  operador: string;
  criadaEm: string;
}

export function venderNoBalcao(venda: NovaVendaBalcao): Promise<VendaBalcao> {
  return httpClient.post<VendaBalcao>("/pdv/vendas", venda);
}

export function listarUltimasVendasDoBalcao(): Promise<VendaBalcao[]> {
  return httpClient.get<VendaBalcao[]>("/pdv/vendas");
}

export function cancelarVendaDoBalcao(pedidoId: string): Promise<VendaBalcao> {
  return httpClient.post<VendaBalcao>(`/pdv/vendas/${pedidoId}/cancelar`, undefined);
}

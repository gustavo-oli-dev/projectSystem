import { httpClient } from "./httpClient.js";

export type TipoItem = "PRODUTO" | "SERVICO";

export interface ItemPedidoRequest {
  tipo: TipoItem;
  referenciaId: string;
  quantidade: number;
}

export interface ItemPedidoResponse {
  tipo: TipoItem;
  referenciaId: string;
  descricao: string;
  precoUnitario: number;
  quantidade: number;
  /** Quanto o item ganhou na promoção do produto (zero = sem promoção). */
  descontoPromocao: number;
  subtotal: number;
}

export type CanalVenda = "PAINEL" | "BALCAO";

/** clienteId e cpfNaNota vêm nulos na venda de balcão sem consumidor identificado. */
export interface Pedido {
  id: string;
  clienteId: string | null;
  canal: CanalVenda;
  cpfNaNota: string | null;
  status: string;
  itens: ItemPedidoResponse[];
  valorTotal: number;
  criadoEm: string;
}

export function criarPedido(clienteId: string, itens: ItemPedidoRequest[]): Promise<Pedido> {
  return httpClient.post<Pedido>("/pedidos", { clienteId, itens });
}

export function confirmarPedido(id: string): Promise<Pedido> {
  return httpClient.post<Pedido>(`/pedidos/${id}/confirmar`, undefined);
}

export function listarPedidos(): Promise<Pedido[]> {
  return httpClient.get<Pedido[]>("/pedidos");
}

export function buscarPedido(id: string): Promise<Pedido> {
  return httpClient.get<Pedido>(`/pedidos/${id}`);
}

export function cancelarPedido(id: string): Promise<Pedido> {
  return httpClient.post<Pedido>(`/pedidos/${id}/cancelar`, undefined);
}

/** Devolve o dinheiro pelo Mercado Pago, cancela o pedido e devolve os produtos ao estoque. */
export function reembolsarPedido(id: string): Promise<Pedido> {
  return httpClient.post<Pedido>(`/pedidos/${id}/reembolsar`, undefined);
}

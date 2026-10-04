/**
 * Último pedido aberto no detalhe. Ao voltar para a lista (pelo botão ou pelo "voltar" do
 * navegador), a lista destaca essa linha. Lido uma vez só: abrir Pedidos pelo menu não destaca nada.
 */
let pedidoEmDestaque: string | null = null;

export function lembrarPedidoEmDestaque(pedidoId: string): void {
  pedidoEmDestaque = pedidoId;
}

export function consumirPedidoEmDestaque(): string | null {
  const pedidoId = pedidoEmDestaque;
  pedidoEmDestaque = null;
  return pedidoId;
}

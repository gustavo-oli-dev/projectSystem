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

/** Comum a toda venda do caixa: produtos, CPF na nota e cliente cadastrado (os dois opcionais). */
export interface DadosVendaBalcao {
  itens: Array<{ produtoId: string; quantidade: number }>;
  cpfNaNota: string | null;
  clienteId: string | null;
}

export interface NovaVendaBalcao extends DadosVendaBalcao {
  pagamento: PagamentoPresencial;
}

/** Como a venda foi paga. PIX_QR = QR code na tela (Mercado Pago); PIX = Pix na maquininha. */
export type FormaVendaBalcao = FormaPagamentoPresencial | "PIX_QR";

export interface VendaBalcao {
  pedidoId: string;
  status: string;
  itens: ItemPedidoResponse[];
  total: number;
  cpfNaNota: string | null;
  formaPagamento: FormaVendaBalcao;
  bandeira: BandeiraCartao | null;
  codigoAutorizacao: string | null;
  maquininhaIntegrada: boolean;
  valorRecebido: number | null;
  troco: number | null;
  statusPagamento: "AGUARDANDO" | "RECUSADO" | "APROVADO" | "ESTORNADO";
  operador: string | null;
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

export interface VendaPixIniciada {
  pedidoId: string;
  total: number;
  qrCodeCopiaECola: string | null;
  qrCodeImagemBase64: string | null;
}

/** Separa os produtos do estoque e cria o Pix no Mercado Pago; devolve o QR para o cliente pagar. */
export function iniciarVendaComPix(venda: DadosVendaBalcao): Promise<VendaPixIniciada> {
  return httpClient.post<VendaPixIniciada>("/pdv/vendas/pix", venda);
}

/** Confere com o Mercado Pago se o Pix já caiu. */
export function acompanharPix(pedidoId: string): Promise<VendaBalcao> {
  return httpClient.get<VendaBalcao>(`/pdv/vendas/${pedidoId}/pix`);
}

/** Maquininha integrada: separa o estoque e manda o valor para a maquininha. */
export function iniciarNaMaquininha(venda: DadosVendaBalcao, forma: FormaPagamentoPresencial): Promise<VendaBalcao> {
  return httpClient.post<VendaBalcao>("/pdv/vendas/maquininha", { ...venda, forma });
}

export function acompanharMaquininha(pedidoId: string): Promise<VendaBalcao> {
  return httpClient.get<VendaBalcao>(`/pdv/vendas/${pedidoId}/maquininha`);
}

/** Cartão recusado: manda o valor de novo (outro cartão). */
export function tentarDeNovoNaMaquininha(pedidoId: string): Promise<VendaBalcao> {
  return httpClient.post<VendaBalcao>(`/pdv/vendas/${pedidoId}/maquininha/tentar-de-novo`, undefined);
}

/** Como uma venda do caixa foi paga (detalhe do pedido). */
export function buscarVendaDoBalcao(pedidoId: string): Promise<VendaBalcao> {
  return httpClient.get<VendaBalcao>(`/pdv/vendas/${pedidoId}`);
}

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
  /** Desconto em reais com o token da autorização do gerente; null = sem desconto. */
  desconto: { valor: number; tokenAutorizacao: string } | null;
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
  /** Desconto autorizado (zero = sem desconto); o total já vem com ele. */
  desconto: number;
  cpfNaNota: string | null;
  formaPagamento: FormaVendaBalcao;
  bandeira: BandeiraCartao | null;
  codigoAutorizacao: string | null;
  maquininhaIntegrada: boolean;
  valorRecebido: number | null;
  troco: number | null;
  statusPagamento: "AGUARDANDO" | "RECUSADO" | "APROVADO" | "ESTORNADO";
  /** E-mail de quem vendeu (identidade). */
  operador: string | null;
  /** Nome para exibir; null em Pix na tela ou usuário removido. */
  operadorNome: string | null;
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

/** O que o gerente pode liberar no caixa com a senha dele. */
export type AcaoAutorizada = "DESCONTO" | "CANCELAR_ITEM";

export interface Autorizacao {
  /** Vale 5 minutos, só para esta ação e este operador. */
  token: string;
  autorizadoPorNome: string;
  expiraEm: string;
}

/** O gerente digita e-mail e senha na hora; a senha não fica guardada — só o token curto. */
export function autorizarNoCaixa(email: string, senha: string, acao: AcaoAutorizada): Promise<Autorizacao> {
  return httpClient.post<Autorizacao>("/pdv/autorizacoes", { email, senha, acao });
}

/** Registra o item tirado da venda (trilha para conferência). */
export function registrarItemCancelado(produtoId: string, quantidade: number, tokenAutorizacao: string): Promise<unknown> {
  return httpClient.post<unknown>("/pdv/itens-cancelados", { produtoId, quantidade, tokenAutorizacao });
}

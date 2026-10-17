import { httpClient } from "./httpClient.js";
import type { Pedido } from "./pedidosApi.js";

export type TipoDocumentoFiscal = "NFE" | "NFSE" | "NFCE";

export const ROTULO_TIPO_DOCUMENTO: Record<TipoDocumentoFiscal, string> = {
  NFE: "NF-e",
  NFSE: "NFS-e",
  NFCE: "NFC-e",
};
export type StatusDocumentoFiscal = "PENDENTE" | "AUTORIZADO" | "REJEITADO" | "CANCELADO";

export interface DocumentoFiscal {
  id: string;
  pedidoId: string;
  tipo: TipoDocumentoFiscal;
  status: StatusDocumentoFiscal;
  protocolo: string | null;
  motivoRejeicao: string | null;
  criadoEm: string;
  atualizadoEm: string;
}

export interface QuantidadeDocumentos {
  status: StatusDocumentoFiscal;
  quantidade: number;
}

export interface ConfiguracaoFiscal {
  ambiente: string;
  certificadoA1Configurado: boolean;
  regimeTributarioDefinido: boolean;
  provedorNfseConfigurado: boolean;
  emissaoHabilitada: boolean;
}

/** Os mais recentes primeiro; o total por status vem de contarDocumentosFiscais. */
export function listarDocumentosFiscais(limite: number): Promise<DocumentoFiscal[]> {
  return httpClient.get<DocumentoFiscal[]>(`/documentos-fiscais?limite=${limite}`);
}

export function contarDocumentosFiscais(): Promise<QuantidadeDocumentos[]> {
  return httpClient.get<QuantidadeDocumentos[]>("/documentos-fiscais/resumo");
}

/** Pedidos confirmados que ainda não tiveram os documentos gerados. */
export function listarPedidosAguardandoDocumentos(limite: number): Promise<Pedido[]> {
  return httpClient.get<Pedido[]>(`/fiscal/pedidos-aguardando?limite=${limite}`);
}

export function listarDocumentosDoPedido(pedidoId: string): Promise<DocumentoFiscal[]> {
  return httpClient.get<DocumentoFiscal[]>(`/pedidos/${pedidoId}/documentos-fiscais`);
}

export function gerarDocumentosFiscais(pedidoId: string): Promise<DocumentoFiscal[]> {
  return httpClient.post<DocumentoFiscal[]>(`/pedidos/${pedidoId}/documentos-fiscais`, undefined);
}

export function consultarConfiguracaoFiscal(): Promise<ConfiguracaoFiscal> {
  return httpClient.get<ConfiguracaoFiscal>("/fiscal/configuracao");
}

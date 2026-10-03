import { httpClient } from "./httpClient.js";

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

export interface ConfiguracaoFiscal {
  ambiente: string;
  certificadoA1Configurado: boolean;
  regimeTributarioDefinido: boolean;
  provedorNfseConfigurado: boolean;
  emissaoHabilitada: boolean;
}

export function listarDocumentosFiscais(): Promise<DocumentoFiscal[]> {
  return httpClient.get<DocumentoFiscal[]>("/documentos-fiscais");
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

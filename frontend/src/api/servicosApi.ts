import { httpClient } from "./httpClient.js";

export interface Servico {
  id: string;
  nome: string;
  descricao: string | null;
  codigoServicoLc116: string;
  aliquotaIss: number;
  precoUnitario: number;
  ativo: boolean;
}

export function listarServicos(): Promise<Servico[]> {
  return httpClient.get<Servico[]>("/servicos");
}

export interface NovoServico {
  nome: string;
  descricao: string | null;
  codigoServicoLc116: string;
  aliquotaIss: number;
  precoUnitario: number;
}

export function cadastrarServico(servico: NovoServico): Promise<Servico> {
  return httpClient.post<Servico>("/servicos", servico);
}

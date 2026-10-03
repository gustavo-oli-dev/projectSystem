import { httpClient } from "./httpClient.js";
import type { Permissao } from "./sessaoApi.js";

export interface Cargo {
  id: string;
  nome: string;
  descricao: string | null;
  permissoes: Permissao[];
}

export interface PermissaoDisponivel {
  codigo: Permissao;
  area: string;
  descricao: string;
}

export interface DadosCargo {
  nome: string;
  descricao: string | null;
  permissoes: Permissao[];
}

export function listarCargos(): Promise<Cargo[]> {
  return httpClient.get<Cargo[]>("/cargos");
}

export function listarPermissoesDisponiveis(): Promise<PermissaoDisponivel[]> {
  return httpClient.get<PermissaoDisponivel[]>("/permissoes");
}

export function criarCargo(dados: DadosCargo): Promise<Cargo> {
  return httpClient.post<Cargo>("/cargos", dados);
}

export function atualizarCargo(id: string, dados: DadosCargo): Promise<Cargo> {
  return httpClient.put<Cargo>(`/cargos/${id}`, dados);
}

export function excluirCargo(id: string): Promise<void> {
  return httpClient.delete<void>(`/cargos/${id}`);
}

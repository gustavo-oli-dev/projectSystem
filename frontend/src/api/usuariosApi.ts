import { httpClient } from "./httpClient.js";

export interface Funcionario {
  id: string;
  nome: string;
  email: string;
  acessoIrrestrito: boolean;
  cargoId: string | null;
  cargoNome: string | null;
  ativo: boolean;
}

export interface NovoFuncionario {
  nome: string;
  email: string;
  senha: string;
  acessoIrrestrito: boolean;
  cargoId: string | null;
}

export function listarFuncionarios(): Promise<Funcionario[]> {
  return httpClient.get<Funcionario[]>("/usuarios");
}

export function cadastrarFuncionario(funcionario: NovoFuncionario): Promise<Funcionario> {
  return httpClient.post<Funcionario>("/usuarios", funcionario);
}

export function trocarCargoDoFuncionario(id: string, cargoId: string): Promise<Funcionario> {
  return httpClient.put<Funcionario>(`/usuarios/${id}/cargo`, { cargoId });
}

export function desativarFuncionario(id: string): Promise<Funcionario> {
  return httpClient.post<Funcionario>(`/usuarios/${id}/desativar`, {});
}

export function ativarFuncionario(id: string): Promise<Funcionario> {
  return httpClient.post<Funcionario>(`/usuarios/${id}/ativar`, {});
}

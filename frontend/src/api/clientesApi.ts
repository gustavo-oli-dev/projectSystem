import { httpClient } from "./httpClient.js";

export interface Cliente {
  id: string;
  nome: string;
  documento: string;
  telefoneWhatsapp: string;
  criadoEm: string;
}

export interface NovoCliente {
  nome: string;
  documento: string;
  telefoneWhatsapp: string;
}

export function listarClientes(): Promise<Cliente[]> {
  return httpClient.get<Cliente[]>("/clientes");
}

export function buscarCliente(id: string): Promise<Cliente> {
  return httpClient.get<Cliente>(`/clientes/${id}`);
}

export function cadastrarCliente(cliente: NovoCliente): Promise<Cliente> {
  return httpClient.post<Cliente>("/clientes", cliente);
}

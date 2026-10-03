import { httpClient } from "./httpClient.js";

export type ModoAtendimento = "BOT" | "HUMANO";
export type OrigemMensagem = "CLIENTE" | "ATENDENTE" | "BOT";

export interface Conversa {
  id: string;
  telefoneWhatsapp: string;
  clienteId: string | null;
  status: "ABERTA" | "ENCERRADA";
  modo: ModoAtendimento;
  atendenteId: string | null;
  motivoTransferencia: string | null;
  criadaEm: string;
  atualizadaEm: string;
}

export interface Mensagem {
  id: string;
  origem: OrigemMensagem;
  conteudo: string | null;
  enviadaEm: string;
}

export function listarConversasAbertas(): Promise<Conversa[]> {
  return httpClient.get<Conversa[]>("/conversas");
}

export function listarMensagens(conversaId: string): Promise<Mensagem[]> {
  return httpClient.get<Mensagem[]>(`/conversas/${conversaId}/mensagens`);
}

export function enviarMensagem(conversaId: string, conteudo: string): Promise<Mensagem> {
  return httpClient.post<Mensagem>(`/conversas/${conversaId}/mensagens`, { conteudo });
}

export function assumirConversa(conversaId: string): Promise<Conversa> {
  return httpClient.post<Conversa>(`/conversas/${conversaId}/assumir`, undefined);
}

export function devolverAoBot(conversaId: string): Promise<Conversa> {
  return httpClient.post<Conversa>(`/conversas/${conversaId}/devolver-ao-bot`, undefined);
}

export function encerrarConversa(conversaId: string): Promise<Conversa> {
  return httpClient.post<Conversa>(`/conversas/${conversaId}/encerrar`, undefined);
}

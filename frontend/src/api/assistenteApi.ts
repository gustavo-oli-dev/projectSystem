import { httpClient } from "./httpClient.js";
import type { Sessao } from "./sessaoApi.js";

interface RespostaAssistente {
  resposta: string;
}

export async function perguntarAoAssistente(pergunta: string): Promise<string> {
  const resultado = await httpClient.post<RespostaAssistente>("/assistente/perguntas", { pergunta });
  return resultado.resposta;
}

/** Envia ao número um código de 6 dígitos pelo WhatsApp (prova de que o número é seu). */
export function enviarCodigoWhatsApp(telefone: string): Promise<void> {
  return httpClient.post<void>("/me/whatsapp/codigo", { telefone });
}

export function confirmarCodigoWhatsApp(codigo: string): Promise<Sessao> {
  return httpClient.post<Sessao>("/me/whatsapp/confirmar", { codigo });
}

export function desvincularWhatsApp(): Promise<Sessao> {
  return httpClient.delete<Sessao>("/me/whatsapp");
}

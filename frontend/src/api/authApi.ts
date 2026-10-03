import { httpClient } from "./httpClient.js";

export interface CredenciaisLogin {
  email: string;
  senha: string;
}

interface RespostaLogin {
  token: string;
}

export async function autenticar(credenciais: CredenciaisLogin): Promise<string> {
  const resposta = await httpClient.post<RespostaLogin>("/auth/login", credenciais, {
    autenticado: false,
  });
  return resposta.token;
}

import { limparToken } from "../state/authState.js";

const BASE_URL = "/api";
const STATUS_NAO_AUTORIZADO = 401;

export class ErroHttp extends Error {
  constructor(
    public readonly status: number,
    message: string
  ) {
    super(message);
  }
}

interface OpcoesRequisicao {
  corpo?: unknown;
  autenticado?: boolean;
}

interface CorpoErro {
  mensagem?: string;
}

async function requisitar<T>(
  metodo: string,
  caminho: string,
  opcoes: OpcoesRequisicao = {}
): Promise<T> {
  const resposta = await enviar(metodo, caminho, opcoes);
  if (resposta.status === 204) {
    return undefined as T;
  }
  return (await resposta.json()) as T;
}

/** Faz a chamada com o token e transforma resposta de erro em ErroHttp (401 encerra a sessão). */
async function enviar(metodo: string, caminho: string, opcoes: OpcoesRequisicao): Promise<Response> {
  // FormData (envio de arquivo): o navegador monta o Content-Type com o "boundary" sozinho.
  const ehArquivo = opcoes.corpo instanceof FormData;
  const cabecalhos: Record<string, string> = ehArquivo ? {} : { "Content-Type": "application/json" };

  if (opcoes.autenticado !== false) {
    const token = obterToken();
    if (token !== null) {
      cabecalhos["Authorization"] = `Bearer ${token}`;
    }
  }

  const resposta = await fetch(`${BASE_URL}${caminho}`, {
    method: metodo,
    headers: cabecalhos,
    body: montarCorpo(opcoes.corpo),
  });

  if (!resposta.ok) {
    if (resposta.status === STATUS_NAO_AUTORIZADO && opcoes.autenticado !== false) {
      // Token ausente/expirado: a sessão acabou. Volta pro login em vez de mostrar erro opaco.
      limparToken();
    }
    const corpoErro: CorpoErro = await resposta.json().catch(() => ({}));
    throw new ErroHttp(resposta.status, corpoErro.mensagem ?? resposta.statusText);
  }
  return resposta;
}

function montarCorpo(corpo: unknown): BodyInit | undefined {
  if (corpo === undefined) {
    return undefined;
  }
  return corpo instanceof FormData ? corpo : JSON.stringify(corpo);
}

function obterToken(): string | null {
  try {
    return localStorage.getItem("token");
  } catch {
    return null;
  }
}

export const httpClient = {
  get: <T>(caminho: string): Promise<T> => requisitar<T>("GET", caminho),
  post: <T>(caminho: string, corpo: unknown, opcoes?: OpcoesRequisicao): Promise<T> =>
    requisitar<T>("POST", caminho, { ...opcoes, corpo }),
  put: <T>(caminho: string, corpo: unknown): Promise<T> => requisitar<T>("PUT", caminho, { corpo }),
  delete: <T>(caminho: string): Promise<T> => requisitar<T>("DELETE", caminho),
  /** Arquivo protegido por login (ex.: CSV): baixa com o token, já que um link comum não o envia. */
  arquivo: async (caminho: string): Promise<Blob> => (await enviar("GET", caminho, {})).blob(),
};

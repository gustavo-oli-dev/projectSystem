import { autenticar } from "../api/authApi.js";
import { ErroHttp } from "../api/httpClient.js";
import { definirToken } from "../state/authState.js";

interface Campo {
  container: HTMLDivElement;
  entrada: HTMLInputElement;
}

export function montarTelaLogin(elementoRaiz: HTMLElement): void {
  const titulo = document.createElement("h2");
  titulo.textContent = "Entrar";

  const formulario = document.createElement("form");

  const campoEmail = criarCampo("email", "E-mail", "email");
  const campoSenha = criarCampo("senha", "Senha", "password");

  const botaoEnviar = document.createElement("button");
  botaoEnviar.type = "submit";
  botaoEnviar.textContent = "Entrar";

  const mensagemErro = document.createElement("p");
  mensagemErro.setAttribute("role", "alert");

  formulario.append(campoEmail.container, campoSenha.container, botaoEnviar, mensagemErro);

  formulario.addEventListener("submit", (evento) => {
    evento.preventDefault();
    void tratarEnvio(campoEmail.entrada, campoSenha.entrada, botaoEnviar, mensagemErro);
  });

  const cartao = document.createElement("div");
  cartao.className = "wizard-card";
  cartao.append(titulo, formulario);

  const container = document.createElement("div");
  container.className = "wizard-container";
  container.append(cartao);

  const centralizador = document.createElement("div");
  centralizador.className = "auth-centralizado";
  centralizador.append(container);

  elementoRaiz.replaceChildren(centralizador);
}

async function tratarEnvio(
  campoEmail: HTMLInputElement,
  campoSenha: HTMLInputElement,
  botaoEnviar: HTMLButtonElement,
  mensagemErro: HTMLParagraphElement
): Promise<void> {
  mensagemErro.textContent = "";
  botaoEnviar.disabled = true;

  try {
    const token = await autenticar({ email: campoEmail.value, senha: campoSenha.value });
    definirToken(token);
  } catch (erro) {
    mensagemErro.textContent = mensagemParaErro(erro);
  } finally {
    botaoEnviar.disabled = false;
  }
}

function mensagemParaErro(erro: unknown): string {
  if (erro instanceof ErroHttp && erro.status === 401) {
    return "E-mail ou senha inválidos.";
  }
  return "Não foi possível conectar ao servidor. Tente novamente.";
}

function criarCampo(id: string, rotuloTexto: string, tipo: string): Campo {
  const container = document.createElement("div");
  container.className = "campo-formulario";

  const rotulo = document.createElement("label");
  rotulo.htmlFor = id;
  rotulo.textContent = rotuloTexto;

  const entrada = document.createElement("input");
  entrada.id = id;
  entrada.type = tipo;
  entrada.required = true;

  container.append(rotulo, entrada);
  return { container, entrada };
}

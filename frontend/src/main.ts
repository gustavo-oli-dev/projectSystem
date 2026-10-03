import { buscarSessao } from "./api/sessaoApi.js";
import { montarTelaLogin } from "./ui/loginView.js";
import { montarShell } from "./ui/shell/appShellView.js";
import { cartaoEstado } from "./ui/estadoCard.js";
import { elementoCarregando } from "./ui/estadoCarregamento.js";
import { estaAutenticado, aoMudarAutenticacao, limparToken } from "./state/authState.js";
import { definirSessao } from "./state/sessaoState.js";

const elementoRaiz = document.getElementById("app");

if (elementoRaiz === null) {
  throw new Error("Elemento #app não encontrado no index.html");
}

void renderizar(elementoRaiz);
aoMudarAutenticacao(() => void renderizar(elementoRaiz));

async function renderizar(raiz: HTMLElement): Promise<void> {
  if (!estaAutenticado()) {
    definirSessao(null);
    montarTelaLogin(raiz);
    return;
  }

  raiz.replaceChildren(elementoCarregando("Carregando seu acesso..."));
  try {
    definirSessao(await buscarSessao());
  } catch {
    // 401 já limpa o token e volta ao login; aqui sobra falha de rede ou servidor fora do ar.
    if (estaAutenticado()) {
      raiz.replaceChildren(criarFalhaSessao());
    }
    return;
  }
  montarShell(raiz);
}

function criarFalhaSessao(): HTMLElement {
  const botaoSair = document.createElement("button");
  botaoSair.type = "button";
  botaoSair.className = "btn btn-ghost btn-pequeno";
  botaoSair.textContent = "Voltar ao login";
  botaoSair.addEventListener("click", () => limparToken());

  const bloco = document.createElement("div");
  bloco.className = "falha-sessao";
  bloco.append(cartaoEstado("Não foi possível carregar seu acesso. Tente recarregar a página.", "erro"), botaoSair);
  return bloco;
}

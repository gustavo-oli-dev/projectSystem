import type { Rota } from "../../router.js";
import { criarIcone } from "../icones.js";

export interface AtalhoNav {
  rota: Rota;
  rotulo: string;
  icone: string;
}

export interface NavInferior {
  elemento: HTMLElement;
  marcarAtivo: (rota: Rota) => void;
  marcarMenuAberto: (aberto: boolean) => void;
  focarMenu: () => void;
}

/** Celular: barra fixa embaixo com os atalhos mais usados e o botão que abre o menu completo. */
export function criarNavInferior(
  atalhos: readonly AtalhoNav[],
  aoNavegar: (rota: Rota) => void,
  aoAlternarMenu: () => void
): NavInferior {
  const botoesAtalho = atalhos.map((atalho) => {
    const botao = criarBotao(atalho.icone, atalho.rotulo);
    botao.dataset["rota"] = atalho.rota;
    botao.addEventListener("click", () => aoNavegar(atalho.rota));
    return botao;
  });

  const botaoMenu = criarBotao("menu-celular", "Menu");
  botaoMenu.setAttribute("aria-expanded", "false");
  botaoMenu.addEventListener("click", aoAlternarMenu);

  const nav = document.createElement("nav");
  nav.className = "nav-inferior";
  nav.setAttribute("aria-label", "Atalhos");
  nav.append(...botoesAtalho, botaoMenu);

  return {
    elemento: nav,
    marcarAtivo: (rota) => {
      for (const botao of botoesAtalho) {
        const ativo = botao.dataset["rota"] === rota;
        botao.classList.toggle("nav-inferior__item--ativo", ativo);
        if (ativo) {
          botao.setAttribute("aria-current", "page");
        } else {
          botao.removeAttribute("aria-current");
        }
      }
    },
    marcarMenuAberto: (aberto) => {
      botaoMenu.classList.toggle("nav-inferior__item--ativo", aberto);
      botaoMenu.setAttribute("aria-expanded", String(aberto));
    },
    focarMenu: () => botaoMenu.focus(),
  };
}

function criarBotao(icone: string, rotulo: string): HTMLButtonElement {
  const texto = document.createElement("span");
  texto.textContent = rotulo;

  const botao = document.createElement("button");
  botao.type = "button";
  botao.className = "nav-inferior__item";
  botao.append(criarIcone(icone), texto);
  return botao;
}

/** Mesmo corte do CSS: abaixo disso a barra lateral vira gaveta aberta pelo botão de menu. */
const TELA_CELULAR = "(max-width: 768px)";
const CLASSE_MENU_ABERTO = "app-shell--menu-aberto";

export function ehTelaCelular(): boolean {
  return window.matchMedia(TELA_CELULAR).matches;
}

export function menuCelularAberto(appShell: HTMLElement): boolean {
  return appShell.classList.contains(CLASSE_MENU_ABERTO);
}

export function definirMenuCelularAberto(appShell: HTMLElement, aberto: boolean): void {
  appShell.classList.toggle(CLASSE_MENU_ABERTO, aberto);
}

/** Camada escura atrás da gaveta: tocar fora do menu fecha. */
export function criarFundoMenuCelular(aoTocar: () => void): HTMLElement {
  const fundo = document.createElement("div");
  fundo.className = "menu-celular__fundo";
  fundo.setAttribute("aria-hidden", "true");
  fundo.addEventListener("click", aoTocar);
  return fundo;
}

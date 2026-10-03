type Ouvinte = (autenticado: boolean) => void;

const ouvintes: Ouvinte[] = [];

export function definirToken(token: string): void {
  try {
    localStorage.setItem("token", token);
  } catch {
    // armazenamento indisponível: a sessão não sobrevive a um reload, mas a aplicação segue
  }
  notificar(true);
}

export function limparToken(): void {
  try {
    localStorage.removeItem("token");
  } catch {
    // armazenamento indisponível, nada a limpar
  }
  notificar(false);
}

export function estaAutenticado(): boolean {
  try {
    return localStorage.getItem("token") !== null;
  } catch {
    return false;
  }
}

export function aoMudarAutenticacao(ouvinte: Ouvinte): void {
  ouvintes.push(ouvinte);
}

function notificar(autenticado: boolean): void {
  for (const ouvinte of ouvintes) {
    ouvinte(autenticado);
  }
}

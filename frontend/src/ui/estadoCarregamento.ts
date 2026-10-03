export function elementoCarregando(mensagem: string): HTMLParagraphElement {
  const paragrafo = document.createElement("p");
  paragrafo.className = "estado-info";
  paragrafo.textContent = mensagem;
  return paragrafo;
}

export function elementoErro(mensagem: string): HTMLParagraphElement {
  const paragrafo = document.createElement("p");
  paragrafo.className = "estado-erro";
  paragrafo.setAttribute("role", "alert");
  paragrafo.textContent = mensagem;
  return paragrafo;
}

export function elementoVazio(mensagem: string): HTMLParagraphElement {
  const paragrafo = document.createElement("p");
  paragrafo.className = "estado-info";
  paragrafo.textContent = mensagem;
  return paragrafo;
}

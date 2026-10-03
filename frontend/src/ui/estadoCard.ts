/**
 * Estado (vazio/erro) que substitui todo o conteúdo de uma seção — sempre dentro de um cartão,
 * nunca texto solto flutuando na página.
 */
export function cartaoEstado(mensagem: string, variante: "vazio" | "erro" = "vazio"): HTMLElement {
  const cartao = document.createElement("div");
  cartao.className = variante === "erro" ? "estado-cartao estado-cartao--erro" : "estado-cartao";
  if (variante === "erro") {
    cartao.setAttribute("role", "alert");
  }

  const texto = document.createElement("p");
  texto.textContent = mensagem;
  cartao.append(texto);

  return cartao;
}

import type { Produto } from "../../api/produtosApi.js";

/** Primeira foto do produto, ou um quadro "Sem foto" do mesmo tamanho. */
export function criarImagemPrincipal(produto: Produto, classe: string): HTMLElement {
  const primeiraFoto = produto.fotos[0];
  if (primeiraFoto === undefined) {
    const semFoto = document.createElement("div");
    semFoto.className = `${classe} sem-foto`;
    semFoto.textContent = "Sem foto";
    return semFoto;
  }
  const imagem = document.createElement("img");
  imagem.className = classe;
  imagem.src = primeiraFoto.url;
  imagem.alt = produto.nome;
  imagem.loading = "lazy";
  return imagem;
}

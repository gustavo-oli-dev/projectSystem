import type { Produto } from "../../api/produtosApi.js";
import { situacaoEstoque } from "./situacaoEstoque.js";

const ROTULO_SEM_ESTOQUE = "Sem estoque";

/**
 * Primeira foto do produto, ou um quadro "Sem foto" do mesmo tamanho. Produto esgotado ganha
 * filtro cinza e o aviso "Sem estoque" centralizado por cima.
 */
export function criarImagemPrincipal(produto: Produto, classe: string): HTMLElement {
  const imagem = criarFotoOuQuadro(produto, classe);
  if (situacaoEstoque(produto.quantidadeEmEstoque).modificador !== "esgotado") {
    return imagem;
  }
  return envolverComAvisoSemEstoque(imagem, classe);
}

function criarFotoOuQuadro(produto: Produto, classe: string): HTMLElement {
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

/** A moldura herda o tamanho (classe original); a foto por dentro só preenche e fica cinza. */
function envolverComAvisoSemEstoque(imagem: HTMLElement, classe: string): HTMLElement {
  imagem.classList.remove(classe);
  imagem.classList.add("foto-sem-estoque__imagem");

  const aviso = document.createElement("span");
  aviso.className = "foto-sem-estoque__aviso";
  aviso.textContent = ROTULO_SEM_ESTOQUE;

  const moldura = document.createElement("div");
  moldura.className = `${classe} foto-sem-estoque`;
  moldura.title = ROTULO_SEM_ESTOQUE;
  moldura.append(imagem, aviso);
  return moldura;
}

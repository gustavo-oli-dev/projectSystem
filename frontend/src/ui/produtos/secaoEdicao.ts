/** Bloco com título usado em cada parte da edição de produto (dados, fotos, estoque). */
export function criarSecao(tituloTexto: string, ...conteudo: HTMLElement[]): HTMLElement {
  const titulo = document.createElement("h2");
  titulo.className = "secao-painel__titulo";
  titulo.textContent = tituloTexto;

  const secao = document.createElement("section");
  secao.className = "formulario-cartao secao-edicao";
  secao.append(titulo, ...conteudo);
  return secao;
}

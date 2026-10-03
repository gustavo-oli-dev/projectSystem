import { alterarQuantidade, itensDoCarrinho, type ItemCarrinho } from "../../state/caixaState.js";
import { formatarMoeda } from "../formatarMoeda.js";

/** Lista da venda em andamento. Quantidade acima do estoque fica marcada (o servidor também recusa). */
export function renderizarCarrinho(area: HTMLElement): void {
  const itens = itensDoCarrinho();
  if (itens.length === 0) {
    area.replaceChildren(criarCarrinhoVazio());
    return;
  }
  const lista = document.createElement("ul");
  lista.className = "carrinho";
  lista.append(...itens.map(criarLinha));
  area.replaceChildren(lista);
}

function criarLinha(item: ItemCarrinho): HTMLLIElement {
  const nome = document.createElement("span");
  nome.className = "carrinho__nome";
  nome.textContent = item.produto.nome;

  const preco = document.createElement("span");
  preco.className = "carrinho__unitario";
  preco.textContent = formatarMoeda(item.produto.precoUnitario);

  const quantidade = document.createElement("input");
  quantidade.type = "number";
  quantidade.min = "0";
  quantidade.max = "999";
  quantidade.value = String(item.quantidade);
  quantidade.className = "carrinho__quantidade";
  quantidade.setAttribute("aria-label", `Quantidade de ${item.produto.nome}`);
  quantidade.addEventListener("change", () => alterarQuantidade(item.produto.id, Math.floor(Number(quantidade.value))));

  const subtotal = document.createElement("span");
  subtotal.className = "carrinho__subtotal";
  subtotal.textContent = formatarMoeda(item.produto.precoUnitario * item.quantidade);

  const remover = document.createElement("button");
  remover.type = "button";
  remover.className = "carrinho__remover";
  remover.textContent = "Remover";
  remover.setAttribute("aria-label", `Remover ${item.produto.nome}`);
  remover.addEventListener("click", () => alterarQuantidade(item.produto.id, 0));

  const linha = document.createElement("li");
  linha.className = "carrinho__linha";
  linha.append(nome, preco, quantidade, subtotal, remover);

  if (item.quantidade > item.produto.quantidadeEmEstoque) {
    linha.classList.add("carrinho__linha--sem-estoque");
    const alerta = document.createElement("span");
    alerta.className = "carrinho__alerta";
    alerta.textContent = `Só ${item.produto.quantidadeEmEstoque} em estoque`;
    linha.append(alerta);
  }
  return linha;
}

/** Estado vazio leve (sem caixa tracejada): só a orientação do que fazer. */
function criarCarrinhoVazio(): HTMLElement {
  const titulo = document.createElement("p");
  titulo.className = "carrinho-vazio__titulo";
  titulo.textContent = "Nenhum produto na venda";
  const dica = document.createElement("p");
  dica.className = "carrinho-vazio__dica";
  dica.textContent = "Leia o código de barras com o leitor ou digite o nome do produto acima.";
  const bloco = document.createElement("div");
  bloco.className = "carrinho-vazio";
  bloco.append(titulo, dica);
  return bloco;
}

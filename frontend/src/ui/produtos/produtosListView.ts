import { listarProdutos, type Produto } from "../../api/produtosApi.js";
import { navegarPara } from "../../router.js";
import { possuiAlguma } from "../../state/sessaoState.js";
import { elementoCarregando } from "../estadoCarregamento.js";
import { cartaoEstado } from "../estadoCard.js";
import { formatarMoeda } from "../formatarMoeda.js";
import { celula, celulaSelo, criarLinha, criarTabela } from "../tabela.js";
import { criarImagemPrincipal } from "./imagemProduto.js";
import { situacaoEstoque } from "./situacaoEstoque.js";

/** Aba Produtos: só visualização (vitrine + estoque). Cadastro e edição ficam em "Gerenciar produtos". */
export async function montarListaProdutos(container: HTMLElement): Promise<void> {
  const titulo = document.createElement("h1");
  titulo.textContent = "Produtos";

  const cabecalho = document.createElement("div");
  cabecalho.className = "cabecalho-pagina";
  cabecalho.append(titulo);

  if (possuiAlguma(["CATALOGO_GERENCIAR", "ESTOQUE_GERENCIAR"])) {
    const botaoGerenciar = document.createElement("button");
    botaoGerenciar.type = "button";
    botaoGerenciar.className = "btn btn-outline btn-pequeno";
    botaoGerenciar.textContent = "Gerenciar produtos";
    botaoGerenciar.addEventListener("click", () => navegarPara("gerenciar-produtos"));
    cabecalho.append(botaoGerenciar);
  }

  const conteudo = document.createElement("div");
  container.replaceChildren(cabecalho, conteudo);
  conteudo.append(elementoCarregando("Carregando produtos..."));

  try {
    const produtos = await listarProdutos();
    renderizar(conteudo, produtos.filter((produto) => produto.ativo));
  } catch {
    conteudo.replaceChildren(cartaoEstado("Não foi possível carregar os produtos.", "erro"));
  }
}

function renderizar(conteudo: HTMLElement, produtos: Produto[]): void {
  if (produtos.length === 0) {
    conteudo.replaceChildren(cartaoEstado("Nenhum produto à venda ainda."));
    return;
  }

  const vitrine = document.createElement("div");
  vitrine.className = "vitrine";
  vitrine.append(...produtos.map(criarCartaoProduto));

  conteudo.replaceChildren(vitrine, criarSecaoEstoque(produtos));
}

function criarCartaoProduto(produto: Produto): HTMLElement {
  const nome = document.createElement("p");
  nome.className = "cartao-produto__nome";
  nome.textContent = produto.nome;

  const preco = document.createElement("p");
  preco.className = "cartao-produto__preco";
  preco.textContent = formatarMoeda(produto.precoUnitario);

  const situacao = situacaoEstoque(produto.quantidadeEmEstoque);
  const selo = document.createElement("span");
  selo.className = `selo selo--${situacao.modificador}`;
  selo.textContent = produto.quantidadeEmEstoque > 0
    ? `${situacao.rotulo} · ${produto.quantidadeEmEstoque} ${produto.unidadeMedida}`
    : situacao.rotulo;

  const corpo = document.createElement("div");
  corpo.className = "cartao-produto__corpo";
  corpo.append(nome, preco, selo);

  const cartao = document.createElement("article");
  cartao.className = "cartao-produto";
  cartao.append(criarImagemPrincipal(produto, "cartao-produto__foto"), corpo);
  return cartao;
}

function criarSecaoEstoque(produtos: Produto[]): HTMLElement {
  const titulo = document.createElement("h2");
  titulo.className = "secao-painel__titulo";
  titulo.textContent = "Estoque";

  const ordenados = [...produtos].sort((a, b) => a.quantidadeEmEstoque - b.quantidadeEmEstoque);
  const linhas = ordenados.map((produto) => {
    const situacao = situacaoEstoque(produto.quantidadeEmEstoque);
    return criarLinha(
      celula(produto.nome),
      celula(produto.codigoBarras ?? "—"),
      celula(`${produto.quantidadeEmEstoque} ${produto.unidadeMedida}`),
      celulaSelo(situacao.rotulo, situacao.modificador)
    );
  });

  const secao = document.createElement("section");
  secao.className = "secao-painel";
  secao.append(titulo, criarTabela(["Produto", "Código de barras", "Quantidade", "Situação"], linhas, "produto(s)"));
  return secao;
}

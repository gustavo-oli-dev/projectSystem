import { buscarProdutoPorCodigoBarras, type Produto } from "../../api/produtosApi.js";
import { formatarMoeda } from "../formatarMoeda.js";

/** Leitor USB/Bluetooth "digita" o código e aperta Enter; dá para digitar o código ou parte do nome. */
const SO_DIGITOS = /^\d{8,14}$/;
const MAXIMO_SUGESTOES = 8;

/**
 * Campo de leitura do caixa. Código de barras vai direto para o carrinho; texto mostra sugestões
 * pelo nome (para produto sem código). Produtos fora de venda nunca aparecem.
 */
export function criarCampoLeitura(
  produtos: readonly Produto[],
  aoEscolher: (produto: Produto) => void
): { elemento: HTMLElement; focar: () => void } {
  const aVenda = produtos.filter((produto) => produto.ativo);
  const porCodigo = new Map<string, Produto>();
  for (const produto of aVenda) {
    if (produto.codigoBarras !== null) {
      porCodigo.set(produto.codigoBarras, produto);
    }
  }

  const entrada = document.createElement("input");
  entrada.type = "text";
  entrada.className = "leitura__campo";
  entrada.placeholder = "Leia o código de barras ou digite o nome do produto";
  entrada.setAttribute("aria-label", "Código de barras ou nome do produto");
  entrada.autocomplete = "off";

  const aviso = document.createElement("p");
  aviso.className = "leitura__aviso";
  aviso.setAttribute("role", "status");

  const sugestoes = document.createElement("div");
  sugestoes.className = "leitura__sugestoes";

  const escolher = (produto: Produto): void => {
    aviso.textContent = "";
    sugestoes.replaceChildren();
    entrada.value = "";
    aoEscolher(produto);
    entrada.focus();
  };

  entrada.addEventListener("input", () => {
    const termo = entrada.value.trim().toLowerCase();
    if (termo.length < 2 || SO_DIGITOS.test(termo)) {
      sugestoes.replaceChildren();
      return;
    }
    const achados = aVenda.filter((produto) => produto.nome.toLowerCase().includes(termo)).slice(0, MAXIMO_SUGESTOES);
    sugestoes.replaceChildren(...achados.map((produto) => criarSugestao(produto, () => escolher(produto))));
  });

  entrada.addEventListener("keydown", (evento) => {
    if (evento.key !== "Enter") {
      return;
    }
    evento.preventDefault();
    const codigo = entrada.value.trim();
    if (!SO_DIGITOS.test(codigo)) {
      const primeira = sugestoes.querySelector<HTMLButtonElement>("button");
      primeira?.click();
      return;
    }
    const local = porCodigo.get(codigo);
    if (local !== undefined) {
      escolher(local);
      return;
    }
    // Pode ser um produto cadastrado depois que a tela abriu.
    buscarProdutoPorCodigoBarras(codigo)
      .then((produto) => {
        if (!produto.ativo) {
          aviso.textContent = `"${produto.nome}" está fora de venda.`;
          return;
        }
        escolher(produto);
      })
      .catch(() => {
        aviso.textContent = `Código ${codigo} não encontrado. Cadastre o produto em Gerenciar produtos.`;
        entrada.select();
      });
  });

  const elemento = document.createElement("div");
  elemento.className = "leitura";
  elemento.append(entrada, sugestoes, aviso);
  return { elemento, focar: () => entrada.focus() };
}

function criarSugestao(produto: Produto, aoClicar: () => void): HTMLButtonElement {
  const nome = document.createElement("span");
  nome.textContent = produto.nome;
  const detalhe = document.createElement("span");
  detalhe.className = "leitura__sugestao-detalhe";
  detalhe.textContent = `${formatarMoeda(produto.precoUnitario)} · ${produto.quantidadeEmEstoque} em estoque`;

  const botao = document.createElement("button");
  botao.type = "button";
  botao.className = "leitura__sugestao";
  botao.append(nome, detalhe);
  botao.addEventListener("click", aoClicar);
  return botao;
}

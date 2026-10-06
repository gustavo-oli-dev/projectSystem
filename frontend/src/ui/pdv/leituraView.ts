import { buscarProdutoPorCodigoBarras, type Embalagem, type Produto } from "../../api/produtosApi.js";
import { formatarMoeda } from "../formatarMoeda.js";

/** Leitor USB/Bluetooth "digita" o código e aperta Enter; dá para digitar o código ou parte do nome. */
const SO_DIGITOS = /^\d{8,14}$/;
const MAXIMO_SUGESTOES = 8;

/** O que foi lido: o produto avulso ou uma embalagem dele (ex.: fardo com 12) — D41. */
interface Leitura {
  produto: Produto;
  embalagem: Embalagem | null;
}

/**
 * Campo de leitura do caixa. Código de barras vai direto para o carrinho (do produto ou da
 * embalagem); texto mostra sugestões pelo nome (para produto sem código), incluindo as embalagens.
 * Produtos fora de venda nunca aparecem.
 */
export function criarCampoLeitura(
  produtos: readonly Produto[],
  aoEscolher: (produto: Produto, embalagem: Embalagem | null) => void
): { elemento: HTMLElement; focar: () => void } {
  const aVenda = produtos.filter((produto) => produto.ativo);
  const leituras: Leitura[] = aVenda.flatMap((produto) => [
    { produto, embalagem: null },
    ...produto.embalagens.map((embalagem) => ({ produto, embalagem })),
  ]);
  const porCodigo = new Map<string, Leitura>();
  for (const leitura of leituras) {
    const codigo = leitura.embalagem === null ? leitura.produto.codigoBarras : leitura.embalagem.codigoBarras;
    if (codigo !== null) {
      porCodigo.set(codigo, leitura);
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

  const escolher = (leitura: Leitura): void => {
    aviso.textContent = "";
    sugestoes.replaceChildren();
    entrada.value = "";
    aoEscolher(leitura.produto, leitura.embalagem);
    entrada.focus();
  };

  entrada.addEventListener("input", () => {
    const termo = entrada.value.trim().toLowerCase();
    if (termo.length < 2 || SO_DIGITOS.test(termo)) {
      sugestoes.replaceChildren();
      return;
    }
    const achadas = leituras.filter((leitura) => leitura.produto.nome.toLowerCase().includes(termo)).slice(0, MAXIMO_SUGESTOES);
    sugestoes.replaceChildren(...achadas.map((leitura) => criarSugestao(leitura, () => escolher(leitura))));
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
        escolher({ produto, embalagem: null });
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

function criarSugestao(leitura: Leitura, aoClicar: () => void): HTMLButtonElement {
  const { produto, embalagem } = leitura;
  const nome = document.createElement("span");
  nome.textContent = embalagem === null ? produto.nome : `${produto.nome} — ${embalagem.nome}`;
  const detalhe = document.createElement("span");
  detalhe.className = "leitura__sugestao-detalhe";
  detalhe.textContent = `${formatarMoeda(embalagem?.preco ?? produto.precoUnitario)} · ${produto.quantidadeEmEstoque} em estoque`;

  const botao = document.createElement("button");
  botao.type = "button";
  botao.className = "leitura__sugestao";
  botao.append(nome, detalhe);
  botao.addEventListener("click", aoClicar);
  return botao;
}

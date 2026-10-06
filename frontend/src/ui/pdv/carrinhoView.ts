import { registrarItemCancelado } from "../../api/pdvApi.js";
import {
  alterarQuantidade,
  chaveDoItem,
  descontoPromocaoDoItem,
  itensDoCarrinho,
  nomeDoItem,
  precoDoItem,
  subtotalDoItem,
  totalDoCarrinho,
  unidadesDoItem,
  type ItemCarrinho,
} from "../../state/caixaState.js";
import { promocaoDoProduto } from "../../state/promocoesDoDia.js";
import { descreverPromocao } from "../produtos/descricaoPromocao.js";
import { pedirAutorizacao } from "./autorizacaoView.js";
import { formatarMoeda } from "../formatarMoeda.js";
import { criarImagemPrincipal } from "../produtos/imagemProduto.js";

const COLUNAS = ["", "Produto", "Preço unit.", "Quantidade", "Subtotal", ""];
const QUANTIDADE_MAXIMA = 999;

/**
 * Itens da venda em andamento, no mesmo formato das tabelas do sistema: cabeçalho, linhas com
 * divisória, rodapé com total de itens. Quantidade acima do estoque fica marcada (o servidor
 * também recusa).
 */
export function renderizarCarrinho(area: HTMLElement): void {
  const itens = itensDoCarrinho();

  const cabecalho = document.createElement("div");
  cabecalho.className = "carrinho__cabecalho";
  cabecalho.append(...COLUNAS.map((titulo) => {
    const coluna = document.createElement("span");
    coluna.textContent = titulo;
    return coluna;
  }));

  const lista = document.createElement("ul");
  lista.className = "carrinho";
  lista.append(...itens.map(criarLinha));

  const moldura = document.createElement("div");
  moldura.className = "carrinho-tabela";
  moldura.append(cabecalho, itens.length === 0 ? criarCarrinhoVazio() : lista, criarRodape(itens));
  area.replaceChildren(moldura);
}

function criarLinha(item: ItemCarrinho): HTMLLIElement {
  const nome = document.createElement("span");
  nome.className = "carrinho__nome";
  nome.textContent = nomeDoItem(item);
  const promocao = item.embalagem === null ? promocaoDoProduto(item.produto.id) : null;
  if (promocao !== null) {
    // Selo da promoção (D38); some o desconto quando ainda não dá direito (ex.: leve 3 e levou 2).
    const selo = document.createElement("span");
    selo.className = "carrinho__promocao";
    selo.textContent = descreverPromocao(promocao);
    nome.append(selo);
  }

  const preco = document.createElement("span");
  preco.className = "carrinho__unitario";
  preco.textContent = formatarMoeda(precoDoItem(item));

  const subtotal = document.createElement("span");
  subtotal.className = "carrinho__subtotal";
  subtotal.textContent = formatarMoeda(subtotalDoItem(item));
  const descontoPromocao = descontoPromocaoDoItem(item);
  if (descontoPromocao > 0) {
    const economia = document.createElement("span");
    economia.className = "carrinho__economia";
    economia.textContent = `− ${formatarMoeda(descontoPromocao)}`;
    subtotal.append(economia);
  }

  const remover = document.createElement("button");
  remover.type = "button";
  remover.className = "btn btn-perigo btn-pequeno";
  remover.textContent = "Remover";
  remover.setAttribute("aria-label", `Remover ${nomeDoItem(item)}`);
  remover.addEventListener("click", () => void mudarQuantidade(item, 0));

  const linha = document.createElement("li");
  linha.className = "carrinho__linha";
  linha.append(
    criarImagemPrincipal(item.produto, "carrinho__foto"),
    nome, preco, criarSeletorQuantidade(item), subtotal, remover
  );

  if (unidadesDoItem(item) > item.produto.quantidadeEmEstoque) {
    linha.classList.add("carrinho__linha--sem-estoque");
    const alerta = document.createElement("span");
    alerta.className = "carrinho__alerta";
    alerta.textContent = `Só ${item.produto.quantidadeEmEstoque} em estoque`;
    linha.append(alerta);
  }
  return linha;
}

/** − [quantidade] + : mais rápido no balcão do que apagar e digitar. */
function criarSeletorQuantidade(item: ItemCarrinho): HTMLElement {
  const menos = criarBotaoQuantidade("−", `Diminuir ${nomeDoItem(item)}`,
    () => void mudarQuantidade(item, item.quantidade - 1));
  const mais = criarBotaoQuantidade("+", `Aumentar ${nomeDoItem(item)}`,
    () => alterarQuantidade(chaveDoItem(item), Math.min(item.quantidade + 1, QUANTIDADE_MAXIMA)));

  const campo = document.createElement("input");
  campo.type = "number";
  campo.min = "0";
  campo.max = String(QUANTIDADE_MAXIMA);
  campo.value = String(item.quantidade);
  campo.className = "seletor-quantidade__campo";
  campo.setAttribute("aria-label", `Quantidade de ${nomeDoItem(item)}`);
  campo.addEventListener("change", () => void mudarQuantidade(item, Math.floor(Number(campo.value))));

  const seletor = document.createElement("div");
  seletor.className = "seletor-quantidade";
  seletor.append(menos, campo, mais);
  return seletor;
}

function criarBotaoQuantidade(rotulo: string, descricao: string, aoClicar: () => void): HTMLButtonElement {
  const botao = document.createElement("button");
  botao.type = "button";
  botao.className = "seletor-quantidade__botao";
  botao.textContent = rotulo;
  botao.setAttribute("aria-label", descricao);
  botao.addEventListener("click", aoClicar);
  return botao;
}

function criarRodape(itens: readonly ItemCarrinho[]): HTMLElement {
  const unidades = itens.reduce((soma, item) => soma + item.quantidade, 0);
  const quantidade = document.createElement("span");
  quantidade.textContent = `${itens.length} produto(s) · ${unidades} unidade(s)`;
  const subtotal = document.createElement("strong");
  subtotal.textContent = `Subtotal ${formatarMoeda(totalDoCarrinho())}`;
  const rodape = document.createElement("div");
  rodape.className = "carrinho__rodape";
  rodape.append(quantidade, subtotal);
  return rodape;
}

/** Estado vazio leve: só a orientação do que fazer. */
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

/**
 * Aumentar é livre. Diminuir ou remover um item já lido é "cancelamento de item" (D35): pede a
 * senha do gerente e registra quem autorizou antes de mexer no carrinho. Se cancelarem a janela
 * ou a autorização falhar, o carrinho fica como estava.
 */
async function mudarQuantidade(item: ItemCarrinho, novaQuantidade: number): Promise<void> {
  const nova = Math.max(0, Number.isFinite(novaQuantidade) ? novaQuantidade : item.quantidade);
  if (nova >= item.quantidade) {
    alterarQuantidade(chaveDoItem(item), Math.min(nova, QUANTIDADE_MAXIMA));
    return;
  }
  const canceladas = item.quantidade - nova;
  const autorizacao = await pedirAutorizacao("CANCELAR_ITEM", "Cancelar item",
    `Tirar ${canceladas} × ${nomeDoItem(item)} da venda precisa da autorização de um gerente.`);
  if (autorizacao === null) {
    alterarQuantidade(chaveDoItem(item), item.quantidade);
    return;
  }
  try {
    // O registro conta unidades do produto: 1 fardo com 12 cancelado = 12 unidades.
    await registrarItemCancelado(item.produto.id, canceladas * (item.embalagem?.unidades ?? 1), autorizacao.token);
    alterarQuantidade(chaveDoItem(item), nova);
  } catch (falha) {
    window.alert(falha instanceof Error && falha.message !== "" ? falha.message : "Não foi possível cancelar o item.");
    alterarQuantidade(chaveDoItem(item), item.quantidade);
  }
}

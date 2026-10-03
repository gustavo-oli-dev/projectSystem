import { listarProdutos } from "../../api/produtosApi.js";
import { listarServicos } from "../../api/servicosApi.js";
import type { TipoItem } from "../../api/pedidosApi.js";
import { adicionarItem, obterEstado, removerItem } from "../../state/pedidoWizardState.js";
import { elementoCarregando, elementoErro, elementoVazio } from "../estadoCarregamento.js";
import { formatarMoeda } from "../formatarMoeda.js";

interface OpcaoCatalogo {
  tipo: TipoItem;
  id: string;
  nome: string;
  precoUnitario: number;
}

export async function montarEtapaItens(container: HTMLElement): Promise<void> {
  const titulo = document.createElement("h2");
  titulo.textContent = "O que o cliente está levando?";

  container.replaceChildren(titulo, elementoCarregando("Carregando catálogo..."));

  try {
    const [produtos, servicos] = await Promise.all([listarProdutos(), listarServicos()]);
    const opcoes: OpcaoCatalogo[] = [
      ...produtos.map((produto) => ({
        tipo: "PRODUTO" as const,
        id: produto.id,
        nome: produto.nome,
        precoUnitario: produto.precoUnitario,
      })),
      ...servicos.map((servico) => ({
        tipo: "SERVICO" as const,
        id: servico.id,
        nome: servico.nome,
        precoUnitario: servico.precoUnitario,
      })),
    ];
    renderizarEtapa(container, titulo, opcoes);
  } catch {
    container.replaceChildren(titulo, elementoErro("Não foi possível carregar produtos e serviços."));
  }
}

function renderizarEtapa(container: HTMLElement, titulo: HTMLElement, opcoes: OpcaoCatalogo[]): void {
  if (opcoes.length === 0) {
    container.replaceChildren(titulo, elementoVazio("Nenhum produto ou serviço cadastrado ainda."));
    return;
  }

  const resumo = document.createElement("div");
  resumo.className = "resumo-itens";

  const catalogo = document.createElement("div");
  catalogo.className = "lista-catalogo";
  catalogo.append(...opcoes.map((opcao) => criarLinhaCatalogo(opcao, resumo)));

  renderizarResumo(resumo);
  container.replaceChildren(titulo, catalogo, resumo);
}

function criarLinhaCatalogo(opcao: OpcaoCatalogo, resumo: HTMLElement): HTMLElement {
  const linha = document.createElement("div");
  linha.className = "linha-catalogo";

  const nome = document.createElement("span");
  nome.className = "linha-catalogo__nome";
  nome.textContent = opcao.nome;

  const preco = document.createElement("span");
  preco.className = "linha-catalogo__preco";
  preco.textContent = formatarMoeda(opcao.precoUnitario);

  const quantidade = document.createElement("input");
  quantidade.type = "number";
  quantidade.min = "1";
  quantidade.value = "1";
  quantidade.className = "linha-catalogo__quantidade";
  quantidade.setAttribute("aria-label", `Quantidade de ${opcao.nome}`);

  const botaoAdicionar = document.createElement("button");
  botaoAdicionar.type = "button";
  botaoAdicionar.className = "btn btn-ghost btn-pequeno";
  botaoAdicionar.textContent = "Adicionar";
  botaoAdicionar.addEventListener("click", () => {
    const quantidadeNumerica = Number(quantidade.value);
    if (!Number.isInteger(quantidadeNumerica) || quantidadeNumerica <= 0) {
      return;
    }
    adicionarItem({
      tipo: opcao.tipo,
      referenciaId: opcao.id,
      descricao: opcao.nome,
      precoUnitario: opcao.precoUnitario,
      quantidade: quantidadeNumerica,
    });
    renderizarResumo(resumo);
  });

  linha.append(nome, preco, quantidade, botaoAdicionar);
  return linha;
}

function renderizarResumo(resumo: HTMLElement): void {
  const itens = obterEstado().itens;

  if (itens.length === 0) {
    resumo.replaceChildren(elementoVazio("Nenhum item selecionado ainda."));
    return;
  }

  const lista = document.createElement("ul");
  lista.className = "resumo-itens__lista";

  itens.forEach((item, indice) => {
    const linha = document.createElement("li");

    const descricao = document.createElement("span");
    descricao.textContent = `${item.quantidade}x ${item.descricao}`;

    const subtotal = document.createElement("span");
    subtotal.textContent = formatarMoeda(item.precoUnitario * item.quantidade);

    const remover = document.createElement("button");
    remover.type = "button";
    remover.className = "botao-remover";
    remover.setAttribute("aria-label", `Remover ${item.descricao}`);
    remover.textContent = "×";
    remover.addEventListener("click", () => {
      removerItem(indice);
      renderizarResumo(resumo);
    });

    linha.append(descricao, subtotal, remover);
    lista.append(linha);
  });

  const valorTotal = itens.reduce((soma, item) => soma + item.precoUnitario * item.quantidade, 0);
  const total = document.createElement("p");
  total.className = "resumo-itens__total";
  total.textContent = `Total: ${formatarMoeda(valorTotal)}`;

  resumo.replaceChildren(lista, total);
}
